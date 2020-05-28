package com.chesapeaketechnology.photomonkey.view;

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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.chesapeaketechnology.photomonkey.R;
import com.chesapeaketechnology.photomonkey.loc.LocationManager;
import com.chesapeaketechnology.photomonkey.loc.LocationManagerProvider;
import com.chesapeaketechnology.photomonkey.model.GalleryManager;
import com.chesapeaketechnology.photomonkey.model.Image;
import com.chesapeaketechnology.photomonkey.model.ImageFileWriter;
import com.chesapeaketechnology.photomonkey.model.MetadataDelegate;
import com.chesapeaketechnology.photomonkey.model.PublicationDelegate;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.ANIMATION_FAST_MILLIS;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.ANIMATION_SLOW_MILLIS;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.KEY_EVENT_ACTION;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.RATIO_16_9_VALUE;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.RATIO_4_3_VALUE;
import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static java.lang.Math.abs;

/**
 * User interface fragment responsible for providing a view finder and capturing pictures.
 *
 * @since 0.1.0
 */
public class CameraFragment extends Fragment {
    private static final String TAG = CameraFragment.class.getSimpleName();

    private LocalBroadcastManager broadcastManager;
    private ConstraintLayout container;
    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private Camera camera;

    private SharedImageViewModel viewModel;

    private int displayId = -1;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private Preview preview;

    private ExecutorService cameraExecutor;

    public CameraFragment(){}

    private DisplayManager getDisplayManager() {
        return (DisplayManager)requireContext().getSystemService(Context.DISPLAY_SERVICE);
    }

    /**
     * Handle presses of the volume down hardware button.
     */
    private final BroadcastReceiver volumeDownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            takePicture();
        }
    };

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */

    private final CameraFragment this_fragment = this;
    private final DisplayListener displayListener = new DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {}

        @Override
        public void onDisplayRemoved(int displayId) {}

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId == this_fragment.displayId) {
                Log.d(TAG, String.format("Rotation changed: %d",
                        requireView().getDisplay().getRotation()));
                if (imageCapture != null) imageCapture.setTargetRotation(requireView().getDisplay().getRotation());
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);

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

    /**
     * Set the thumbnail used in the gallery button in the user interface.
     * @param uri
     */
    private void setGalleryThumbnail(Uri uri) {
        // Reference of the view that holds the gallery thumbnail
        ImageView thumbnail = container.findViewById(R.id.photo_view_button);
        thumbnail.post(new Runnable() {
            @Override
            public void run() {
                Uri imageUri = uri;
                if (imageUri.getScheme() == null) {
                    imageUri = Uri.parse("file://" + imageUri.getPath());
                }
                int dimension = (int) getResources().getDimension(R.dimen.stroke_small);
                thumbnail.setPadding(dimension, dimension, dimension, dimension);
                Glide.with(thumbnail)
                        .load(imageUri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(thumbnail);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Create a shared view model to facilitate passing data between fragments.
        viewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);

        //Register the shared view model to receive location updates.
        LocationManager locationManager = ((LocationManagerProvider)requireActivity()).getLocationManager();
        viewModel.setLocationManager(locationManager);

        container = (ConstraintLayout) view;
        viewFinder = container.findViewById(R.id.view_finder);

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        broadcastManager = LocalBroadcastManager.getInstance(view.getContext());

        // Set up the intent filter that will receive events from our main activity
        IntentFilter filter = new IntentFilter();
        filter.addAction(KEY_EVENT_ACTION);
        broadcastManager.registerReceiver(volumeDownReceiver, filter);

        // Every time the orientation of device changes, update rotation for use cases
        getDisplayManager().registerDisplayListener(displayListener, null);

        // Wait for the views to be properly laid out
        viewFinder.post(() -> {
            // Keep track of the display in which this view is attached
            displayId = viewFinder.getDisplay().getDisplayId();

            // Build UI controls
            updateCameraUi();

            // Bind use cases
            bindCameraUseCases();
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
        Log.d(TAG, String.format("Screen metrics: %d x %d", metrics.widthPixels, metrics.heightPixels));

        double screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);
        Log.d(TAG, String.format("Preview aspect ratio: %f", screenAspectRatio));

        int rotation = viewFinder.getDisplay().getRotation();

        // Bind the CameraProvider to the LifeCycleOwner
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // setup the Preview
                preview = new Preview.Builder()
                        // We request aspect ratio but no resolution
                        .setTargetAspectRatio((int)screenAspectRatio)
                        // Set initial target rotation
                        .setTargetRotation(rotation)
                        .build();

                // configure the ImageCapture component
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        // We request aspect ratio but no resolution to match preview config, but letting
                        // CameraX optimize for whatever specific resolution best fits our use cases
                        .setTargetAspectRatio((int)screenAspectRatio)
                        // Set initial target rotation, we will have to call this again if rotation changes
                        // during the lifecycle of this use case
                        .setTargetRotation(rotation)
                        .build();

               // Must unbind the use-cases before rebinding them
               cameraProvider.unbindAll();
               try {
                   // A variable number of use-cases can be passed here -
                   // camera provides access to CameraControl & CameraInfo
                   // may need to add focus and metering in the future.
                   camera = cameraProvider.bindToLifecycle(this_fragment, cameraSelector, preview, imageCapture);

                   // Attach the viewfinder's surface provider to preview use case
                   if ( preview != null ){
                       preview.setSurfaceProvider(viewFinder.createSurfaceProvider(camera.getCameraInfo()));
                   }
               } catch(Exception exc) {
                   Log.e(TAG, "Use case binding failed", exc);
               }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "bindCameraUseCases: Unable to get camera provider", e);
                viewFinder.post(() -> {
                    Toast.makeText(requireContext(), String.format("Unable to get camera provider. %s", e.getMessage()), Toast.LENGTH_SHORT).show();
                });
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

    /**
     * Re-draw the camera UI controls; called every time configuration changes.
     */
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
        try {
            Uri latestUri = new GalleryManager().getLatest();
            if (latestUri != null) {
                setGalleryThumbnail(latestUri);
            }
        } catch (GalleryManager.GalleryAccessFailure e) {
            Log.e(TAG, "updateCameraUi: Unable to find existing images.", e);
            Throwable rootCause = Throwables.getRootCause(e);
            viewFinder.post(() -> {
                Toast.makeText(requireContext(), String.format("Unable to find existing images. %s", rootCause.getMessage()), Toast.LENGTH_SHORT).show();
            });
        }

        // Listener for button used to capture photo
        controls.findViewById(R.id.camera_capture_button).setOnClickListener(v -> takePicture());

        // Listener for button used to switch cameras
        controls.findViewById(R.id.camera_switch_button).setOnClickListener(v -> {
            if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            } else {
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            }
            // Re-bind use cases to update selected camera
            bindCameraUseCases();
        });

        // Listener for button used to view the most recent photo
        controls.findViewById(R.id.photo_view_button).setOnClickListener(v -> {
            // Only navigate when the gallery has photos
            GalleryManager galleryManager = new GalleryManager();
            try {
                if(! galleryManager.isEmpty()){
                    Navigation.findNavController(requireActivity(), R.id.fragment_container)
                            .navigate(CameraFragmentDirections.actionCameraFragmentToGalleryFragment());
                }
            } catch (GalleryManager.GalleryAccessFailure galleryAccessFailure) {
                Log.e(TAG, "updateCameraUi: Unable to find existing images.", galleryAccessFailure);
                Throwable rootCause = Throwables.getRootCause(galleryAccessFailure);
                viewFinder.post(() -> {
                    Toast.makeText(requireContext(), String.format("Unable to find existing images. %s", rootCause.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Capture a picture from the camera, save it, update the metadata, and
     * request user input for supplementary data.
     */
    private void takePicture() {
        // Get a stable reference of the modifiable image capture use case
        if (imageCapture != null) {
            imageCapture.takePicture(cameraExecutor,
                    new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                            Image image;
                            try {
                                image = Image.create(imageProxy);
                                viewModel.setImage(image);
                                // if the current value of lensFacing is front, then the image is horizontally reversed.
                                viewModel.setReversed((lensFacing == CameraSelector.LENS_FACING_FRONT));
                                Uri savedUri = image.getUri();
                                Log.d(TAG, String.format("Photo capture succeeded: %s", savedUri));
                                // We can only change the foreground Drawable using API level 23+ API
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    setGalleryThumbnail(savedUri);
                                }
                                image.publish();
                                Navigation.findNavController(requireActivity(), R.id.fragment_container)
                                        .navigate(CameraFragmentDirections.actionCameraFragmentToSupplementaryInputFragment());
                            } catch (ImageFileWriter.FormatNotSupportedException unlikely) {
                                // OnCaptureSuccess doc says "The image is of format ImageFormat.JPEG". So, this should never happen.
                                // https://developer.android.com/reference/androidx/camera/core/ImageCapture.OnImageCapturedListener#onCaptureSuccess(androidx.camera.core.ImageProxy,%20int)
                                Log.wtf(TAG, "Format not supported.", unlikely);
                                viewFinder.post(() -> {
                                    Toast.makeText(requireContext(), String.format("Camera capture format not supported. %s", unlikely.getMessage()), Toast.LENGTH_LONG).show();
                                });
                            } catch (ImageFileWriter.WriteException writeException) {
                                Log.e(TAG, "Unable to save image.", writeException);
                                viewFinder.post(() -> {
                                    Toast.makeText(requireContext(), String.format("Unable to save image. %s", writeException.getMessage()), Toast.LENGTH_LONG).show();
                                });
                            } catch (MetadataDelegate.ReadFailure readFailure) {
                                Log.w(TAG, "Unable to read image metadata.", readFailure);
                                viewFinder.post(() -> {
                                    Toast.makeText(requireContext(), String.format("Unable to read image metadata. %s", readFailure.getMessage()), Toast.LENGTH_SHORT).show();
                                });
                            } catch (PublicationDelegate.PublicationFailure publicationFailure) {
                                Log.e(TAG, "Unable to publish image.", publicationFailure);
                                viewFinder.post(() -> {
                                    Toast.makeText(requireContext(), String.format("Unable to publish image. %s", publicationFailure.getMessage()), Toast.LENGTH_LONG).show();
                                });
                            } finally {
                                imageProxy.close();
                            }
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "onError: Unable to capture image.", exception);
                            viewFinder.post(() -> {
                                Toast.makeText(requireContext(), String.format("Unable to capture image. %s", exception.getMessage()), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                container.postDelayed(() -> {
                    container.setForeground(new ColorDrawable(Color.WHITE));
                    container.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            container.setForeground(null);
                        }
                    }, ANIMATION_FAST_MILLIS);
                }, ANIMATION_SLOW_MILLIS);
            }
        }
    }
}
