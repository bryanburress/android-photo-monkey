package com.chesapeaketechnology.photomonkey.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager.widget.ViewPager;

import com.chesapeaketechnology.photomonkey.R;
import com.chesapeaketechnology.photomonkey.model.AMetadataDelegate;
import com.chesapeaketechnology.photomonkey.model.GalleryManager;
import com.chesapeaketechnology.photomonkey.model.Image;
import com.chesapeaketechnology.photomonkey.model.PublicationDelegate;
import com.google.android.datatransport.BuildConfig;
import com.google.common.base.Throwables;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import timber.log.Timber;

/**
 * Fragment responsible for allowing users to see, edit, delete, and share existing photos.
 *
 * @since 0.2.0
 */
public class GalleryFragment extends Fragment
{
    private final GalleryManager galleryManager;
    private List<Uri> mediaList = new ArrayList<>();

    public GalleryFragment()
    {
        galleryManager = new GalleryManager();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        //Get images from the gallery manager and load into list.
        try
        {
            mediaList = galleryManager.getMedia();
        } catch (GalleryManager.GalleryAccessFailure e)
        {
            Timber.e(e, "updateCameraUi: Unable to find existing images.");
            Throwable rootCause = Throwables.getRootCause(e);
            requireView().post(() -> {
                Toast.makeText(requireContext(), String.format("Unable to find existing images. %s", rootCause.getMessage()), Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        //Checking media files list
        if (mediaList.isEmpty())
        {
            view.findViewById(R.id.delete_button).setEnabled(false);
            view.findViewById(R.id.share_button).setEnabled(false);
            view.findViewById(R.id.edit_button).setEnabled(false);
            view.findViewById(R.id.upload_button).setEnabled(false);
        }

        // Populate the ViewPager and implement a cache of two media items
        ViewPager mediaViewPager = view.findViewById(R.id.photo_view_pager);
        mediaViewPager.setOffscreenPageLimit(2);
        mediaViewPager.setAdapter(new MediaPagerAdapter(getChildFragmentManager()));

        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            View cutoutSafeView = view.findViewById(R.id.cutout_safe_area);
            WindowInsets rootInsets = cutoutSafeView.getRootWindowInsets();
            if (rootInsets != null)
            {
                DisplayCutout cutout = rootInsets.getDisplayCutout();
                if (cutout != null)
                {
                    cutoutSafeView.setPadding(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(), cutout.getSafeInsetRight(), cutout.getSafeInsetBottom());
                }
            }
            cutoutSafeView.setOnApplyWindowInsetsListener((v, insets) -> {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null)
                {
                    cutoutSafeView.setPadding(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(), cutout.getSafeInsetRight(), cutout.getSafeInsetBottom());
                }
                return insets;
            });
        }

        // ***** Back *****
        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp();
        });

        // ***** Share *****
        view.findViewById(R.id.share_button).setOnClickListener(v -> {
            Uri mediaUri = mediaList.get(mediaViewPager.getCurrentItem());
            if (mediaUri != null)
            {
                // Create a sharing intent
                Intent intent = new Intent();
                try
                {
                    Image img = Image.create(mediaUri);
                    // Set the appropriate intent extra, type, action and flags
                    intent.setType("image/jpg");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_STREAM, mediaUri);

                    // Launch the intent letting the user choose which app to share with
                    startActivity(Intent.createChooser(intent, "Share using"));
                } catch (AMetadataDelegate.ReadFailure shareFailure)
                {
                    Timber.e(shareFailure, "bindCameraUseCases: Unable to get create share intent");
                    requireView().post(() -> {
                        Toast.makeText(requireContext(), String.format("Unable to share photo. %s", shareFailure.getMessage()), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });

        // ***** Edit *****
        view.findViewById(R.id.edit_button).setOnClickListener(v -> {
            Uri mediaUri = mediaList.get(mediaViewPager.getCurrentItem());
            // Use the shared view model to pass the image to the input fragment.
            SharedImageViewModel model = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
            try
            {
                Image img = Image.create(mediaUri);
                model.setImage(img);
                Navigation.findNavController(requireActivity(), R.id.fragment_container)
                        .navigate(GalleryFragmentDirections.actionGalleryFragmentToSupplementaryInputFragment());
            } catch (AMetadataDelegate.ReadFailure e)
            {
                Timber.e(e, "Unable to edit photo. %s", e.getMessage());
                view.post(() -> {
                    Toast.makeText(requireContext(), String.format("Unable to edit photo. %s", e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        });

        // ***** Upload to Sync Monkey and Azure DataFileReceiver *****
        view.findViewById(R.id.upload_button).setOnClickListener(v -> {
            Uri mediaUri = mediaList.get(mediaViewPager.getCurrentItem());
            try
            {
                Image img = Image.create(mediaUri);
                PublicationDelegate pd = new PublicationDelegate();
                pd.sendToSyncMonkey(img);
                PublicationDelegate.uploadFileToRemoteEndpoint(mediaUri);
            } catch (AMetadataDelegate.ReadFailure | PublicationDelegate.PublicationFailure e)
            {
                Timber.e(e, "Unable to publish photo. %s", e.getMessage());
                view.post(() -> {
                    Toast.makeText(requireContext(), String.format("Unable to publish photo. %s", e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        });

        // ***** Delete *****
        view.findViewById(R.id.delete_button).setOnClickListener(v -> {
            Uri mediaUri = mediaList.get(mediaViewPager.getCurrentItem());
            if (mediaUri != null)
            {
                galleryManager.discard(view.getContext(), mediaUri,
                        (Consumer<Uri>) uri -> {
                            mediaList.remove(mediaViewPager.getCurrentItem());
                            Objects.requireNonNull(mediaViewPager.getAdapter()).notifyDataSetChanged();
                            if (mediaList.isEmpty())
                            {
                                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp();
                            }
                        },
                        (Consumer<Uri>) uri -> {
                            Timber.i("User cancelled media delete operation.");
                        },
                        (Consumer<Exception>) discardException -> {
                            Timber.e(discardException, "Unable to delete photo. %s", discardException.getMessage());
                            view.post(() -> {
                                Toast.makeText(requireContext(), String.format("Unable to delete photo. %s", discardException.getMessage()), Toast.LENGTH_LONG).show();
                            });
                        }
                );
            }
        });
    }

    /**
     * Paging adapter for the images in the gallery.
     *
     * @since 0.2.0
     */
    class MediaPagerAdapter extends FragmentStatePagerAdapter
    {
        public MediaPagerAdapter(FragmentManager fm)
        {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position)
        {
            return PhotoFragment.create(mediaList.get(position));
        }

        @Override
        public int getCount()
        {
            return mediaList.size();
        }

        @Override
        public int getItemPosition(@NonNull Object object)
        {
            return POSITION_NONE;
        }
    }
}
