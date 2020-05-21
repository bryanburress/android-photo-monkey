package com.chesapeaketechnology.photomonkey.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.chesapeaketechnology.photomonkey.PhotoMonkeyActivity;
import com.chesapeaketechnology.photomonkey.R;
import com.chesapeaketechnology.photomonkey.image.AsyncImageDataProvider;
import com.chesapeaketechnology.photomonkey.image.ImageCaptureCallbackListener;
import com.chesapeaketechnology.photomonkey.image.ImageDataResultListener;
import com.chesapeaketechnology.photomonkey.image.ImageSavedListener;
import com.chesapeaketechnology.photomonkey.util.LocationHelper;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.ANIMATION_FAST_MILLIS;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.ANIMATION_SLOW_MILLIS;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.EXTENSION_WHITELIST;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.FILENAME;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.KEY_EVENT_ACTION;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PHOTO_EXTENSION;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.RATIO_16_9_VALUE;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.RATIO_4_3_VALUE;
import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static java.lang.Math.abs;


public class CameraFragment extends Fragment {
    private static final String LOG_TAG = CameraFragment.class.getSimpleName();

    private File outputDirectory;
    private LocalBroadcastManager broadcastManager;
    private ConstraintLayout container;
    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private LocationHelper locationHelper;
    private Location lastLocation;

    private int displayId = -1;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private Preview preview;

    private ExecutorService cameraExecutor;

    public CameraFragment(){}

    private DisplayManager getDisplayManager() {
        return (DisplayManager)requireContext().getSystemService(Context.DISPLAY_SERVICE);
    }

    private BroadcastReceiver volumeDownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */

    private final CameraFragment this_fragment = this;
    private DisplayListener displayListener = new DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {}

        @Override
        public void onDisplayRemoved(int displayId) {}

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId ==this_fragment.displayId) {
                Log.d(LOG_TAG, String.format("Rotation changed: %d",
                        requireView().getDisplay().getRotation()));
                if (imageCapture != null) imageCapture.setTargetRotation(requireView().getDisplay().getRotation());
                if (imageAnalyzer != null) imageAnalyzer.setTargetRotation(requireView().getDisplay().getRotation());
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera_view, container, false);

    }

    @Override
    public void onResume() {
        super.onResume();
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    CameraFragmentDirections.actionCameraFragmentToPermissionsFragment()
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Shut down our background executor
        cameraExecutor.shutdown();

        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver);
        getDisplayManager().unregisterDisplayListener(displayListener);
    }

    private void setGalleryThumbnail(Uri uri) {
        // Reference of the view that holds the gallery thumbnail
        ImageView thumbnail = container.findViewById(R.id.photo_view_button);
        thumbnail.post(new Runnable() {
            @Override
            public void run() {
                int dimension = (int) getResources().getDimension(R.dimen.stroke_small);
                thumbnail.setPadding(dimension, dimension, dimension, dimension);
                Glide.with(thumbnail)
                        .load(uri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(thumbnail);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = (ConstraintLayout) view;
        viewFinder = container.findViewById(R.id.view_finder);

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        broadcastManager = LocalBroadcastManager.getInstance(view.getContext());

        locationHelper = new LocationHelper(view.getContext());
        locationHelper.startListeningUserLocation(location -> {
            lastLocation = location;
        });

        // Set up the intent filter that will receive events from our main activity
        IntentFilter filter = new IntentFilter();
        filter.addAction(KEY_EVENT_ACTION);
        broadcastManager.registerReceiver(volumeDownReceiver, filter);

        // Every time the orientation of device changes, update rotation for use cases
        getDisplayManager().registerDisplayListener(displayListener, null);

        // Determine the output directory
        outputDirectory = PhotoMonkeyActivity.getOutputDirectory(requireContext());

        // Wait for the views to be properly laid out
        viewFinder.post(new Runnable() {
            @Override
            public void run() {
                // Keep track of the display in which this view is attached
                int displayId = viewFinder.getDisplay().getDisplayId();

                // Build UI controls
                updateCameraUi();

                // Bind use cases
                bindCameraUseCases();
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateCameraUi();
    }

    /** Declare and bind preview, capture and analysis use cases */
    private void bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        DisplayMetrics metrics = new DisplayMetrics();
        viewFinder.getDisplay().getRealMetrics(metrics);
        Log.d(LOG_TAG, String.format("Screen metrics: %d x %d", metrics.widthPixels, metrics.heightPixels));

        double screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);
        Log.d(LOG_TAG, String.format("Preview aspect ratio: %f", screenAspectRatio));

        int rotation = viewFinder.getDisplay().getRotation();

        // Bind the CameraProvider to the LifeCycleOwner
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        ListenableFuture cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(new Runnable() {
             @Override
             public void run() {
                 // CameraProvider
                 try {
                     ProcessCameraProvider cameraProvider = (ProcessCameraProvider)cameraProviderFuture.get();
                     // Preview
                     preview = new Preview.Builder()
                             // We request aspect ratio but no resolution
                             .setTargetAspectRatio((int)screenAspectRatio)
                             // Set initial target rotation
                             .setTargetRotation(rotation)
                             .build();

                     // ImageCapture
                     imageCapture = new ImageCapture.Builder()
                             .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                             // We request aspect ratio but no resolution to match preview config, but letting
                             // CameraX optimize for whatever specific resolution best fits our use cases
                             .setTargetAspectRatio((int)screenAspectRatio)
                             // Set initial target rotation, we will have to call this again if rotation changes
                             // during the lifecycle of this use case
                             .setTargetRotation(rotation)
                             .build();

                     // ImageAnalysis
                     imageAnalyzer = new ImageAnalysis.Builder()
                             // We request aspect ratio but no resolution
                             .setTargetAspectRatio((int)screenAspectRatio)
                             // Set initial target rotation, we will have to call this again if rotation changes
                             // during the lifecycle of this use case
                             .setTargetRotation(rotation)
                             .build();

                             // The analyzer can then be assigned to the instance
//                     ImageAnalysisConfig imgAConfig = new androidx.camera.core.impl.ImageAnalysisConfig.Builder()
                             //.setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE).build();

                     imageAnalyzer.setAnalyzer(cameraExecutor, image -> {
                         int rotationDegrees = image.getImageInfo().getRotationDegrees();
                            // insert your code here.
                         Log.d(LOG_TAG, String.format("Rotation degrees: %d", rotationDegrees));
                     });
                    // Must unbind the use-cases before rebinding them
                    cameraProvider.unbindAll();
                    try {
                        // A variable number of use-cases can be passed here -
                        // camera provides access to CameraControl & CameraInfo
                        camera = cameraProvider.bindToLifecycle(this_fragment, cameraSelector, preview, imageCapture, imageAnalyzer);

                        // Attach the viewfinder's surface provider to preview use case
                        if ( preview != null ){
                            preview.setSurfaceProvider(viewFinder.createSurfaceProvider(camera.getCameraInfo()));
                        }
                    } catch(Exception exc) {
                        Log.e(LOG_TAG, "Use case binding failed", exc);
                    }
                 } catch (ExecutionException e) {
                     e.printStackTrace();
                 } catch (InterruptedException e) {
                     e.printStackTrace();
                 }

            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private int aspectRatio(int width, int height) {
        double previewRatio = (double) max(width, height) / min(width, height);
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    private void showDescriptionDialog(DescriptionDialogResultListener listener) {
        FragmentManager fm = getParentFragmentManager();
        DescriptionDialogFragment dialog = new DescriptionDialogFragment();
        dialog.addListener(listener);
        dialog.show(fm, "description_dialog_view");

    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    @SuppressLint("StaticFieldLeak")
    private void updateCameraUi() {

        // Remove previous UI if any
        View tmpView = container.findViewById(R.id.camera_ui_container);
        if (tmpView != null) {
            container.removeView(tmpView);
        }

        // Inflate a new view containing all UI for controlling the camera
        View controls = View.inflate(requireContext(), R.layout.camera_ui_container, container);


        // In the background, load latest photo taken (if any) for gallery thumbnail
        new AsyncTask<Void, Void, File[]>() {
            @Override
            protected File[] doInBackground(Void... voids) {
                if (outputDirectory != null) {
                    File[] files = outputDirectory.listFiles((dir, name) -> {
                        String extension = Files.getFileExtension(name);
                        return EXTENSION_WHITELIST.contains(extension.toUpperCase(Locale.ROOT));
                    });
                    return files;
                } else {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(File[] files) {
                if (files != null && files.length > 0) {
                    File lastFile = files[files.length - 1];
                    setGalleryThumbnail(Uri.fromFile(lastFile));
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


        // Listener for button used to capture photo
        controls.findViewById(R.id.camera_capture_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get a stable reference of the modifiable image capture use case
                if (imageCapture != null) {
                    File photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION);
                    imageCapture.takePicture(cameraExecutor,
                            new ImageCaptureCallbackListener(photoFile, lensFacing,
                                new AsyncImageDataProvider() {
                                    @Override
                                    public void requestDescription(ImageDataResultListener resultListener) {
                                          showDescriptionDialog(new DescriptionDialogResultListener() {
                                            @Override
                                            public void onSaveDescription(String description) {
                                                Log.d(LOG_TAG, "Saved image adding a description");
                                                resultListener.onData(description, lastLocation);
                                            }

                                            @Override
                                            public void onCloseWithoutSaving() {
                                                Log.d(LOG_TAG, "Closed without adding a description");
                                            }
                                        });
                                    }
                                }, new ImageSavedListener() {
                                    @Override
                                    public void onSaved(File photoFile) {
                                        Uri savedUri = Uri.fromFile(photoFile);
                                        Log.d(LOG_TAG, String.format("Photo capture succeeded: %s", savedUri));

                                        // We can only change the foreground Drawable using API level 23+ API
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            setGalleryThumbnail(savedUri);
                                        }

                                        // Implicit broadcasts will be ignored for devices running API level >= 24
                                        // so if you only target API level 24+ you can remove this statement
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                            requireActivity().sendBroadcast(
                                                    new Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                                            );
                                        }

                                        // If the folder selected is an external media directory, this is
                                        // unnecessary but otherwise other apps will not be able to access our
                                        // images unless we scan them using [MediaScannerConnection]
                                        File savedFile = new File(savedUri.getPath());
                                        String extension = Files.getFileExtension(savedFile.getName());
                                        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                                        MediaScannerConnection.scanFile(requireContext(), new String[]{savedFile.getAbsolutePath()}, new String[]{mimeType}, (path, uri) -> {
                                            Log.d(LOG_TAG, String.format("Image capture scanned into media store: %s", uri));
                                        });
                                    }
                                }
                            )
                    );

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        container.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                container.setForeground(new ColorDrawable(Color.WHITE));
                                container.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        container.setForeground(null);
                                    }
                                }, ANIMATION_FAST_MILLIS);
                            }
                        }, ANIMATION_SLOW_MILLIS);
                    }
                }
            }
        });


        // Listener for button used to switch cameras
        controls.findViewById(R.id.camera_switch_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                } else {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                }
                // Re-bind use cases to update selected camera
                bindCameraUseCases();
            }
        });

        // Listener for button used to view the most recent photo
        controls.findViewById(R.id.photo_view_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Only navigate when the gallery has photos
                File[] fileList = outputDirectory.listFiles();
                if (fileList != null && fileList.length > 0) {
                    //TODO: - Add the gallery view
                    Navigation.findNavController(requireActivity(), R.id.fragment_container)
                            .navigate(CameraFragmentDirections.actionCameraFragmentToGalleryFragment(outputDirectory.getAbsolutePath()));
                }
            }
        });
    }

    private static File createFile(File baseFolder, String format, String extension) {
        return new File(baseFolder, new SimpleDateFormat(format, Locale.US).format(System.currentTimeMillis()) + extension);
    }
}
