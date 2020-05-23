package com.chesapeaketechnology.photomonkey.view;

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

import java.io.File;

public class PhotoFragment extends Fragment {
    public static final String FILE_NAME_KEY = "file_name";

    public PhotoFragment(){}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return new ImageView(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if(args == null) return;
        File resource = new File(args.getString(FILE_NAME_KEY));
        if (resource == null) {
            int resource_id = R.drawable.ic_photo;
            Glide.with(view).load(resource_id).into((ImageView) view);
        } else {
            Glide.with(view).load(resource).into((ImageView) view);
        }
    }

    static PhotoFragment create(File image) {
        PhotoFragment frag = new PhotoFragment();
        Bundle arguments = new Bundle();
        arguments.putString(FILE_NAME_KEY, image.getAbsolutePath());
        frag.setArguments(arguments);
        return frag;
    }
}
