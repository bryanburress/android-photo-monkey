package com.chesapeaketechnology.photomonkey.view;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.chesapeaketechnology.photomonkey.R;

/**
 * Down samples, scales, and loads the provided image uri into an ImageView
 *
 * @since 0.2.0
 */
public class PhotoFragment extends Fragment
{
    public static final String FILE_NAME_KEY = "file_name";

    public PhotoFragment()
    {
    }

    /**
     * Create a {@link PhotoFragment} for a given image {@link Uri}.
     *
     * @param imageUri The {@linl Uri} for the image.
     * @return a {@link PhotoFragment} object with the provided image.
     */
    static PhotoFragment create(Uri imageUri)
    {
        PhotoFragment frag = new PhotoFragment();
        Bundle arguments = new Bundle();
        arguments.putString(FILE_NAME_KEY, imageUri.toString());
        frag.setArguments(arguments);
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return new ImageView(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve the image uri from the file name passed on the transition.
        Bundle args = getArguments();
        if (args == null) return;
        String path = args.getString(FILE_NAME_KEY);
        Uri imageUri = Uri.parse(path);
        // add a file scheme prefix if the path is an absolute file path.
        if (imageUri.getScheme() == null)
        {
            imageUri = Uri.parse("file://" + imageUri.getPath());
        }
        if (imageUri == null)
        {
            // Render the default icon if the uri is null
            Glide.with(view).load(R.drawable.ic_photo).centerCrop().into((ImageView) view);
        } else
        {
            Glide.with(view).load(imageUri).centerCrop().into((ImageView) view);
        }
    }
}
