package com.chesapeaketechnology.photomonkey.view;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chesapeaketechnology.photomonkey.R;
import com.chesapeaketechnology.photomonkey.model.AMetadataDelegate;
import com.chesapeaketechnology.photomonkey.model.Image;
import com.chesapeaketechnology.photomonkey.model.Metadata;

/**
 * Provides the necessary form elements for capturing supplementary data about an image
 * from a user. It populates the resulting data into the shared view model as a
 * new {@link Metadata} object.
 *
 * @since 0.2.0
 */
public class SupplementaryInputFragment extends Fragment
{
    private static final String TAG = SupplementaryInputFragment.class.getSimpleName();

    public SupplementaryInputFragment()
    {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_supplementary_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        SharedImageViewModel model = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
        EditText descriptionField = view.findViewById(R.id.descriptionField);
        if (model.getImage() != null)
        {
            ImageView imageView = view.findViewById(R.id.backgroundPreview);
            Uri imageUri = model.getImage().getUri();
            if (imageUri.getScheme() == null)
            {
                imageUri = Uri.parse("file://" + imageUri.getPath());
            }
            Glide.with(this)
                    .load(imageUri)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .into(imageView);
            String description = model.getImage().getMetadata().getDescription();
            if (!TextUtils.isEmpty(description))
            {
                descriptionField.setText(description);
            } else
            {
                descriptionField.setText("");
            }
        }
        view.findViewById(R.id.saveButton).setOnClickListener(e -> {
            try
            {
                String description = descriptionField.getText().toString();
                if (!TextUtils.isEmpty(description))
                {
                    Metadata metadata = new Metadata(description, model.getLastLocation(), model.isReversed());
                    Image image = model.getImage().updateMetadata(metadata);
                    model.setImage(image);
                }
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp();
            } catch (AMetadataDelegate.SaveFailure mse)
            {
                Log.e(TAG, "onViewCreated: Unable to save metadata.", mse);
                Toast.makeText(requireContext(), String.format("Unable to save metadata. %s", mse.getMessage()), Toast.LENGTH_LONG).show();
            }
        });

        view.findViewById(R.id.close_button).setOnClickListener(v -> {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp();
        });
    }
}
