package com.chesapeaketechnology.photomonkey.view;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.chesapeaketechnology.photomonkey.R;
import com.chesapeaketechnology.photomonkey.loc.ILocationManagerProvider;
import com.chesapeaketechnology.photomonkey.loc.LocationManager;
import com.chesapeaketechnology.photomonkey.model.AMetadataDelegate;
import com.chesapeaketechnology.photomonkey.model.GalleryManager;
import com.chesapeaketechnology.photomonkey.model.Image;
import com.chesapeaketechnology.photomonkey.model.ImageFileWriter;
import com.chesapeaketechnology.photomonkey.model.PublicationDelegate;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.FOCUS_RECT_SIZE;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.FOCUS_STROKE_WIDTH;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.KEY_EVENT_ACTION;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.RATIO_16_9_VALUE;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.RATIO_4_3_VALUE;
import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static java.lang.Math.abs;

import timber.log.Timber;

/**
 * User interface fragment responsible for providing a view finder and capturing pictures. This is
 * the primary view for the application. It manages setting up, controlling, and capturing images
 * from the devices camera.
 *
 * @since 0.2.0
 */
public class CameraFragment extends Fragment
{
    private final CameraFragment this_fragment = this;
    private LocalBroadcastManager broadcastManager;
    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private Camera camera;
    private SharedImageViewModel viewModel;
    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private final DisplayListener displayListener = new DisplayListener()
    {
        @Override
        public void onDisplayAdded(int displayId)
        {
        }

        @Override
        public void onDisplayRemoved(int displayId)
        {
        }

        @Override
        public void onDisplayChanged(int displayId)
        {
            if (displayId == this_fragment.displayId)
            {
                Timber.d("Rotation changed: %d", requireView().getDisplay().getRotation());
                if (imageCapture != null)
                {
                    imageCapture.setTargetRotation(requireView().getDisplay().getRotation());
                }
            }
        }
    };
    private FrameLayout container;
    private FocusRectangleView focusView;
    private MediaPlayer shutterSound;
    private ToggleButton focusButton;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private Preview preview;
    private int displayId = -1;
    private ExecutorService cameraExecutor;
    /**
     * Takes picture when the volume down hardware button is pressed.
     * <p>
     * This event is caught and rebroadcast by the Activity.
     */
    private final BroadcastReceiver volumeDownReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            takePicture();
        }
    };

    public CameraFragment()
    {
    }

    /**
     * Get the display manager service so we can listen for display change events.
     *
     * @return the {@link DisplayManager} system service
     */
    private DisplayManager getDisplayManager()
    {
        return (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext()))
        {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    CameraFragmentDirections.actionCameraFragmentToPermissionsFragment()
            );
        }
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        // Shut down our background executor
        cameraExecutor.shutdown();

        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver);
        getDisplayManager().unregisterDisplayListener(displayListener);
    }

    /**
     * Set the thumbnail used in the gallery button in the user interface.
     *
     * @param uri the {@link Uri} for the image to display.
     */
    private void setGalleryThumbnail(Uri uri)
    {
        // Reference of the view that holds the gallery thumbnail
        ImageView thumbnail = container.findViewById(R.id.photo_view_button);
        thumbnail.post(() -> {
            Uri imageUri = uri;
            if (imageUri.getScheme() == null)
            {
                imageUri = Uri.parse("file://" + imageUri.getPath());
            }
            int dimension = (int) getResources().getDimension(R.dimen.stroke_small);
            thumbnail.setPadding(dimension, dimension, dimension, dimension);
            Glide.with(thumbnail)
                    .load(imageUri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(thumbnail);
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        // Create a shared view model to facilitate passing data between fragments.
        viewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);

        //Register the shared view model to receive location updates.
        LocationManager locationManager = ((ILocationManagerProvider) requireActivity()).getLocationManager();
        viewModel.setLocationManager(locationManager);

        container = (FrameLayout) view;
        viewFinder = container.findViewById(R.id.view_finder);

        // Setup the focus overlay rectangle
        focusView = container.findViewById(R.id.view_finder_overlay);
        focusView.setColor(Color.valueOf(Color.WHITE));
        focusView.setStrokeWidth(FOCUS_STROKE_WIDTH);

        // Initialize our background executor
        cameraExecutor = Executors.newCachedThreadPool();

        // Set up the intent filter that will receive events from our main activity
        // Used take pictures when volume down is pressed.
        broadcastManager = LocalBroadcastManager.getInstance(view.getContext());
        IntentFilter filter = new IntentFilter();
        filter.addAction(KEY_EVENT_ACTION);
        broadcastManager.registerReceiver(volumeDownReceiver, filter);

        // Every time the orientation of device changes, update rotation for use cases
        getDisplayManager().registerDisplayListener(displayListener, null);

        // Create a player for the camera shutter sound effect.
        shutterSound = MediaPlayer.create(requireContext(), R.raw.camera_shutter_click);

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
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        updateCameraUi();
    }

    /**
     * Enable Auto Focus, Auto White Balance, and Auto Exposure based
     * on a point in the center of the screen. Updates every second.
     * This will disable manual focus if it is engaged.
     */
    private void startAutoFocus()
    {
        try
        {
            // Cancel any existing focus and metering operations
            camera.getCameraControl().cancelFocusAndMetering();
            // Update the auto focus button
            focusButton.setChecked(true);

            MeteringPointFactory meteringPointFactory = viewFinder.getMeteringPointFactory();

            // Create an auto focus point at the center of the screen.
            float centerX = (float) viewFinder.getWidth() / 2;
            float centerY = (float) viewFinder.getHeight() / 2;
            MeteringPoint autoFocusPoint = meteringPointFactory.createPoint(centerX, centerY);

            // Use AutoFocus, AutoExposure, and Auto White Balance
            int meteringMode = FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB;
            FocusMeteringAction focusMeteringAction =
                    new FocusMeteringAction.Builder(autoFocusPoint, meteringMode)
                            .setAutoCancelDuration(1, TimeUnit.SECONDS)
                            .build();
            camera.getCameraControl().startFocusAndMetering(focusMeteringAction);

            // Hide the focus rect when in auto focus mode.
            focusView.clear();
        } catch (Throwable t)
        {
            Timber.e(t, "setupCameraUseCases: error in autofocus");
        }
    }

    /**
     * While called manual focus, technically, this is still auto focus.  However,
     * it moves the auto focus point from the center of the screen to the provided
     * point on the screen.
     *
     * @param center the {@link Point} where the AF, AE, & AWB should be determined.
     */
    private void focusOn(Point center)
    {
        camera.getCameraControl().cancelFocusAndMetering();
        focusButton.setChecked(false);
        MeteringPointFactory meteringPointFactory = viewFinder.getMeteringPointFactory();
        MeteringPoint meteringPoint = meteringPointFactory.createPoint(center.x, center.y);
        int meteringMode = FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB;
        FocusMeteringAction tapToFocusMeteringAction =
                new FocusMeteringAction.Builder(meteringPoint, meteringMode)
                        .disableAutoCancel()
                        .build();
        camera.getCameraControl().startFocusAndMetering(tapToFocusMeteringAction);
        focusView.setFocusLocation(center, FOCUS_RECT_SIZE);
    }

    /**
     * Declare and bind preview, capture and analysis use cases
     */
    @SuppressLint("ClickableViewAccessibility")
    private void bindCameraUseCases()
    {

        // Get screen metrics used to setup camera for full screen resolution
        DisplayMetrics metrics = new DisplayMetrics();
        viewFinder.getDisplay().getRealMetrics(metrics);
        Timber.d("Screen metrics: %d x %d", metrics.widthPixels, metrics.heightPixels);

        int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);
        Timber.d("Preview aspect ratio: %d", screenAspectRatio);

        int rotation = viewFinder.getDisplay().getRotation();

        // Bind the CameraProvider to the LifeCycleOwner
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        // Create a detector to detect pinch to zoom gestures
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(requireContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener()
                {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector)
                    {
                        ZoomState zoomState = camera.getCameraInfo().getZoomState().getValue();
                        float currentZoomRatio = (zoomState != null) ? zoomState.getZoomRatio() : 1f;
                        float delta = detector.getScaleFactor();
                        camera.getCameraControl().setZoomRatio(currentZoomRatio * delta);
                        return true;
                    }
                });

        // Create a detector that detects tap gestures (helps avoid being triggered by other unrelated gestures).
        GestureDetector tapGestureDetector = new GestureDetector(requireContext(),
                new GestureDetector.SimpleOnGestureListener()
                {
                    @Override
                    public boolean onSingleTapUp(MotionEvent event)
                    {
                        focusOn(new Point((int) event.getX(), (int) event.getY()));
                        return true;
                    }
                }
        );

        //TODO: - Implement accessibility support for gestures and taps.
        viewFinder.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            tapGestureDetector.onTouchEvent(event);
            return true;
        });

        // When camera provider is ready
        cameraProviderFuture.addListener(() -> {
            try
            {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // setup the Preview
                preview = new Preview.Builder()
                        // We request aspect ratio but no resolution
                        .setTargetAspectRatio(screenAspectRatio)
                        // Set initial target rotation
                        .setTargetRotation(rotation)
                        .build();

                // configure the ImageCapture component
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        // We request aspect ratio but no resolution to match preview config, but letting
                        // CameraX optimize for whatever specific resolution best fits our use cases
                        .setTargetAspectRatio(screenAspectRatio)
                        // Set the initial flash mode based on shared view model (retains state in
                        // transitions between fragments)
                        .setFlashMode(viewModel.getFlashMode())
                        // Set initial target rotation, we will have to call this again if rotation changes
                        // during the lifecycle of this use case
                        .setTargetRotation(rotation)
                        .build();

                // Must unbind the use-cases before rebinding them
                cameraProvider.unbindAll();
                try
                {
                    // A variable number of use-cases can be passed here -
                    // camera provides access to CameraControl & CameraInfo
                    // may need to add focus and metering in the future.
                    camera = cameraProvider.bindToLifecycle(this_fragment, cameraSelector, preview, imageCapture);

                    // Attach the viewfinder's surface provider to preview use case
                    if (preview != null)
                    {
                        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
                    }
                    // Start auto focus at center of screen
                    startAutoFocus();
                } catch (Exception exc)
                {
                    Timber.e(exc, "Use case binding failed");
                    viewFinder.post(() -> Toast.makeText(requireContext(), String.format("Unable to initialize camera. %s", exc.getMessage()), Toast.LENGTH_SHORT).show());
                }
            } catch (ExecutionException | InterruptedException e)
            {
                Timber.e(e, "bindCameraUseCases: Unable to get camera provider");
                viewFinder.post(() -> Toast.makeText(requireContext(), String.format("Unable to get camera provider. %s", e.getMessage()), Toast.LENGTH_SHORT).show());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     * [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     * <p>
     * Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     * of preview ratio to one of the provided values.
     * </p>
     *
     * @param width  - preview width
     * @param height - preview height
     * @return suitable aspect ratio
     */
    private int aspectRatio(int width, int height)
    {
        double previewRatio = (double) max(width, height) / min(width, height);
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE))
        {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    /**
     * Update the icon on the flash button to correctly reflect the current state.
     *
     * @param flashMode   The current flash mode {@link ImageCapture.FlashMode}
     * @param flashButton A reference to the flash button.
     */
    private void updateFlashIcon(int flashMode, ImageButton flashButton)
    {
        Drawable flashIcon;
        switch (flashMode)
        {
            case ImageCapture.FLASH_MODE_AUTO:
                flashIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_flash_auto);
                break;
            case ImageCapture.FLASH_MODE_OFF:
                flashIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_flash_off);
                break;
            case ImageCapture.FLASH_MODE_ON:
                flashIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_flash_on);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + flashMode);
        }
        flashButton.setImageDrawable(flashIcon);
    }

    /**
     * Re-draw the camera UI controls; called every time configuration changes.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void updateCameraUi()
    {
        // Remove previous UI if any
        View tmpView = container.findViewById(R.id.camera_ui_container);
        if (tmpView != null)
        {
            container.removeView(tmpView);
        }

        // Inflate a new view containing all UI for controlling the camera
        View controls = View.inflate(requireContext(), R.layout.camera_ui_container, container);

        // Load latest photo taken (if any) for gallery thumbnail
        try
        {
            Uri latestUri = new GalleryManager().getLatest();
            if (latestUri != null)
            {
                setGalleryThumbnail(latestUri);
            }
        } catch (GalleryManager.GalleryAccessFailure e)
        {
            Timber.e(e, "updateCameraUi: Unable to find existing images.");
            Throwable rootCause = Throwables.getRootCause(e);
            viewFinder.post(() -> Toast.makeText(requireContext(), String.format("Unable to find existing images. %s", rootCause.getMessage()), Toast.LENGTH_SHORT).show());
        }


        ImageButton prefsButton = controls.findViewById(R.id.prefsButton);
        prefsButton.setOnClickListener(v -> openPreferences());

        // Update the flashButton to reflect the current state.
        ImageButton flashButton = controls.findViewById(R.id.flashButton);
        updateFlashIcon(viewModel.getFlashMode(), flashButton);
        // Create a detector that detects tap gestures for the flash button.
        GestureDetector flashButtonTapDetector = new GestureDetector(requireContext(),
                new GestureDetector.SimpleOnGestureListener()
                {
                    @Override
                    public boolean onSingleTapUp(MotionEvent event)
                    {
                        int flashMode = viewModel.getFlashMode();
                        flashMode = ++flashMode % 3; // cycle through the 3 flash modes defined on ImageCapture
                        viewModel.setFlashMode(flashMode);
                        updateFlashIcon(viewModel.getFlashMode(), flashButton);
                        return true;
                    }
                }
        );
        //TODO: - Fix accessibility for on touch
        // Handle changes to the flash button state.
        flashButton.setOnTouchListener((v, event) -> flashButtonTapDetector.onTouchEvent(event));

        // Handle changes to the focus button state
        focusButton = controls.findViewById(R.id.focusButton);
        focusButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
            {
                startAutoFocus();
            } else
            {
                focusOn(new Point(viewFinder.getWidth() / 2, viewFinder.getHeight() / 2));
            }
        });

        // Listener for button used to capture photo
        controls.findViewById(R.id.camera_capture_button).setOnClickListener(v -> takePicture());

        // Listener for button used to switch cameras
        controls.findViewById(R.id.camera_switch_button).setOnClickListener(v -> {
            if (CameraSelector.LENS_FACING_FRONT == lensFacing)
            {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            } else
            {
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            }
            // Re-bind use cases to update selected camera
            bindCameraUseCases();
        });

        // Listener for button used to view the most recent photo
        controls.findViewById(R.id.photo_view_button).setOnClickListener(v -> {
            // Only navigate when the gallery has photos
            GalleryManager galleryManager = new GalleryManager();
            try
            {
                if (!galleryManager.isEmpty())
                {
                    Navigation.findNavController(requireActivity(), R.id.fragment_container)
                            .navigate(CameraFragmentDirections.actionCameraFragmentToGalleryFragment());
                }
            } catch (GalleryManager.GalleryAccessFailure galleryAccessFailure)
            {
                Timber.e(galleryAccessFailure, "updateCameraUi: Unable to find existing images.");
                Throwable rootCause = Throwables.getRootCause(galleryAccessFailure);
                viewFinder.post(() -> Toast.makeText(requireContext(), String.format("Unable to find existing images. %s", rootCause.getMessage()), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void openPreferences()
    {
        Fragment settingsFragment = new SettingsFragment();
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, settingsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Capture a picture from the camera, save it, update the metadata, and
     * request user input for supplementary data.
     */
    private void takePicture()
    {
        if (imageCapture != null)
        {
//            container.postDelayed(() -> {
//                AudioManager audio = (AudioManager) requireActivity().getSystemService(Context.AUDIO_SERVICE);
//                if (audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL)
//                {
//                    shutterSound.start();
//                }
//            }, ANIMATION_FAST_MILLIS);

            // set the flash mode to the current state in the shared view model
            // flash doesn't work consistently without setting this here.
            imageCapture.setFlashMode(viewModel.getFlashMode());
            imageCapture.takePicture(cameraExecutor,
                    new ImageCapture.OnImageCapturedCallback()
                    {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy imageProxy)
                        {
                            AudioManager audio = (AudioManager) requireActivity().getSystemService(Context.AUDIO_SERVICE);
                            if (audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL)
                            {
                                shutterSound.start();
                            }

                            Image image = null;
                            try
                            {
                                // create an image model object
                                image = Image.create(imageProxy);
                                viewModel.setImage(image);
                                // if the current value of lensFacing is front, then the image is horizontally reversed.
                                viewModel.setReversed((lensFacing == CameraSelector.LENS_FACING_FRONT));
                                Uri savedUri = image.getUri();

                                Timber.d(savedUri.getPath(), "Photo capture succeeded: %s");
                                setGalleryThumbnail(savedUri);

                                try
                                {
                                    // Publish the image to other services
                                    image.publish();
                                } catch (PublicationDelegate.PublicationFailure publicationFailure)
                                {
                                    Timber.e(publicationFailure, "Unable to publish image.");
                                    viewFinder.post(() ->
                                            Toast.makeText(requireContext(), String.format("Unable to publish image. %s", publicationFailure.getMessage()), Toast.LENGTH_LONG).show());
                                }
                                // Automatically navigate to edit the supplementary data.
                                new Handler(Looper.getMainLooper()).post(() ->
                                        Navigation.findNavController(requireActivity(), R.id.fragment_container)
                                                .navigate(CameraFragmentDirections.actionCameraFragmentToSupplementaryInputFragment()));

                            } catch (ImageFileWriter.FormatNotSupportedException unlikely)
                            {
                                // OnCaptureSuccess doc says "The image is of format ImageFormat.JPEG". So, this should never happen.
                                // https://developer.android.com/reference/androidx/camera/core/ImageCapture.OnImageCapturedListener#onCaptureSuccess(androidx.camera.core.ImageProxy,%20int)
                                Timber.wtf(unlikely, "Format not supported.");
                                viewFinder.post(() ->
                                        Toast.makeText(requireContext(), String.format("Camera capture format not supported. %s", unlikely.getMessage()), Toast.LENGTH_LONG).show());
                            } catch (ImageFileWriter.WriteException writeException)
                            {
                                Timber.e(writeException, "Unable to save image.");
                                viewFinder.post(() ->
                                        Toast.makeText(requireContext(), String.format("Unable to save image. %s", writeException.getMessage()), Toast.LENGTH_LONG).show());
                            } catch (AMetadataDelegate.ReadFailure readFailure)
                            {
                                Timber.w(readFailure, "Unable to read image metadata.");
                                viewFinder.post(() ->
                                        Toast.makeText(requireContext(), String.format("Unable to read image metadata. %s", readFailure.getMessage()), Toast.LENGTH_SHORT).show());
                            } finally
                            {
                                imageProxy.close();
                            }
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception)
                        {
                            Timber.e(exception, "onError: Unable to capture image.");
                            viewFinder.post(() ->
                                    Toast.makeText(requireContext(), String.format("Unable to capture image. %s", exception.getMessage()), Toast.LENGTH_SHORT).show());
                        }
                    }
            );
        }
    }
}
