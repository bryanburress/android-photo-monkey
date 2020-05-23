package com.chesapeaketechnology.photomonkey.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.chesapeaketechnology.photomonkey.R;

import java.util.Arrays;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PERMISSIONS_REQUEST_CODE;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PERMISSIONS_REQUIRED;

public class PermissionsFragment extends Fragment {

    public PermissionsFragment(){}

    public static boolean hasPermissions(Context context) {
        return Arrays.stream(PERMISSIONS_REQUIRED).allMatch( p -> {
            return ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED;
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!hasPermissions(requireContext())) {
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE);
        } else {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment()
            );
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                Toast.makeText(getContext(), "Permission request granted", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                        PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment()
                );
            } else {
                Toast.makeText(getContext(), "Permission request denied", Toast.LENGTH_LONG).show();
            }
        }
    }
}
