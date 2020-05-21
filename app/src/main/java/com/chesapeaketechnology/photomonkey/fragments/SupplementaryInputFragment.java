package com.chesapeaketechnology.photomonkey.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.chesapeaketechnology.photomonkey.R;
import com.chesapeaketechnology.photomonkey.sdata.SupplementaryInputData;
import com.chesapeaketechnology.photomonkey.sdata.SupplementaryInputDelegate;

import java.util.ArrayList;

public class SupplementaryInputFragment extends DialogFragment {
    private static final String LOG_TAG = SupplementaryInputFragment.class.getSimpleName();

    private ArrayList<SupplementaryInputDelegate> resultListeners = new ArrayList<SupplementaryInputDelegate>();
    AlertDialog dialog;

    public void addListener(SupplementaryInputDelegate listener) {
        //there can be only one
        resultListeners = new ArrayList<>();
        resultListeners.add(listener);
    }

    public void removeListener(SupplementaryInputDelegate listener) {
        resultListeners.remove(listener);
    }

//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.ThemeOverlay_Material_Dark);
//    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        try {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Unable to set background for dialog", e);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        View dialogView = inflater.inflate(R.layout.description_dialog_view, null);
        EditText descriptionField = dialogView.findViewById(R.id.descriptionField);
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(dialogView)
                .setTitle(getResources().getString(R.string.description_dialog_title))
                .setPositiveButton(R.string.description_dialog_positive_button, (dialog, id) -> {
                    resultListeners.forEach(listener -> {
                        listener.receivedInput(new SupplementaryInputData(descriptionField.getText().toString()));
                    });
                });
        return builder.create();
    }


}
