package com.chesapeaketechnology.photomonkey.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.chesapeaketechnology.photomonkey.R;

import java.util.ArrayList;

public class DescriptionDialogFragment extends DialogFragment {

    private ArrayList<DescriptionDialogResultListener> resultListeners = new ArrayList<DescriptionDialogResultListener>();
    AlertDialog dialog;

    public void addListener(DescriptionDialogResultListener listener) {
        //there can be only one
        resultListeners = new ArrayList<>();
        resultListeners.add(listener);
    }

    public void removeListener(DescriptionDialogResultListener listener) {
        resultListeners.remove(listener);
    }

//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.ThemeOverlay_Material_Dark);
//    }

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
                        listener.onSaveDescription(descriptionField.getText().toString());
                    });
                });
//                .setNegativeButton(R.string.description_dialog_negative_button, (DialogInterface.OnClickListener) (dialog, id) -> {
//                    DescriptionDialogFragment.this.getDialog().cancel();
//                    resultListeners.forEach(DescriptionDialogResultListener::onCloseWithoutSaving);
//                });
        return builder.create();
    }


}
