package com.chesapeaketechnology.photomonkey.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.navigation.Navigation;
import androidx.viewpager.widget.ViewPager;

import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;

import com.chesapeaketechnology.photomonkey.BuildConfig;
import com.chesapeaketechnology.photomonkey.R;
import com.google.common.io.Files;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.EXTENSION_WHITELIST;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.FLAGS_FULLSCREEN;

public class GalleryFragment extends Fragment {
    private GalleryFragmentArgs args;
    private List<File> mediaList = new ArrayList<>();

    class MediaPagerAdapter extends FragmentStatePagerAdapter {
        public MediaPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return PhotoFragment.create(mediaList.get(position));
        }

        @Override
        public int getCount() {
            return mediaList.size();
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }
    }

    public GalleryFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        args = GalleryFragmentArgs.fromBundle(requireArguments());
        setRetainInstance(true);
        if(args != null) {
            File rootDirectory = new File(args.getRootDirectory());

            // Walk through all files in the root directory
            // We reverse the order of the list to present the last photos first
            List<File> files = (List<File>) Arrays.asList(Objects.requireNonNull(
                    rootDirectory.listFiles((dir, name) -> {
                        String extension = Files.getFileExtension(name);
                        return EXTENSION_WHITELIST.contains(extension.toUpperCase(Locale.ROOT));
                    })
                    )
            );
            Collections.sort(files, Collections.reverseOrder());
            mediaList = files;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Checking media files list
        if(mediaList.isEmpty()) {
          view.findViewById(R.id.delete_button).setEnabled(false);
          view.findViewById(R.id.share_button).setEnabled(false);
        }

        // Populate the ViewPager and implement a cache of two media items
        ViewPager mediaViewPager = view.findViewById(R.id.photo_view_pager);
        mediaViewPager.setOffscreenPageLimit(2);
        mediaViewPager.setAdapter(new MediaPagerAdapter(getChildFragmentManager()));

        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            View cutoutSafeView = view.findViewById(R.id.cutout_safe_area);
            WindowInsets rootInsets = cutoutSafeView.getRootWindowInsets();
            if(rootInsets != null) {
               DisplayCutout cutout = rootInsets.getDisplayCutout();
               if (cutout != null) {
                   cutoutSafeView.setPadding(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(), cutout.getSafeInsetRight(), cutout.getSafeInsetBottom());
               }
            }
            cutoutSafeView.setOnApplyWindowInsetsListener((v, insets) -> {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    cutoutSafeView.setPadding(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(), cutout.getSafeInsetRight(), cutout.getSafeInsetBottom());
                }
                return insets;
            });
        }

        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp();
        });

        view.findViewById(R.id.share_button).setOnClickListener(v -> {
            File mediaFile = mediaList.get(mediaViewPager.getCurrentItem());
            if(mediaFile != null) {
                // Create a sharing intent
                Intent intent = new Intent();
                // Infer media type from file extension
                String extension = Files.getFileExtension(mediaFile.getName());
                String mediaType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                Uri uri = FileProvider.getUriForFile(view.getContext(), BuildConfig.APPLICATION_ID + ".provider", mediaFile);
                // Set the appropriate intent extra, type, action and flags
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setType(mediaType);
                intent.setAction(Intent.ACTION_SEND);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Launch the intent letting the user choose which app to share with
                startActivity(Intent.createChooser(intent, "Share using"));
            }
        });

        view.findViewById(R.id.delete_button).setOnClickListener(v -> {
            File mediaFile = mediaList.get(mediaViewPager.getCurrentItem());
            if(mediaFile != null) {
                AlertDialog dialog = new AlertDialog.Builder(view.getContext(), android.R.style.Theme_Material_Dialog)
                        .setTitle("Confirm")
                        .setMessage("Delete current photo?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, (_dialog, which) -> {
                            mediaFile.delete();
                            MediaScannerConnection.scanFile(view.getContext(), new String[]{mediaFile.getAbsolutePath()}, null, null);
                            mediaList.remove(mediaViewPager.getCurrentItem());
                            Objects.requireNonNull(mediaViewPager.getAdapter()).notifyDataSetChanged();
                            if(mediaList.isEmpty()) {
                                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();
                Window window = dialog.getWindow();
                if(window != null) {
                    // Set the dialog to not focusable
                    window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                    // Make sure that the dialog's window is in full screen
                    View decorView = window.getDecorView();
                    if(decorView != null) {
                        decorView.setSystemUiVisibility(FLAGS_FULLSCREEN);
                    }
                }
                // Show the dialog while still in immersive mode
                dialog.show();
                if(window != null) {
                    // Set the dialog to focusable again
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                }

            }
        });

    }
}
