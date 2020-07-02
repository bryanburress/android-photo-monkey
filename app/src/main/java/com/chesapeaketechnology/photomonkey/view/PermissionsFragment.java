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

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.*;

/**
 * Verifies that the necessary permissions are present and navigates back to the {@link CameraFragment}.
 *
 * @since 0.2.0
 */
public class PermissionsFragment extends Fragment
{

    public PermissionsFragment()
    {
    }

    /**
     * Check of the application has the permissions defined in {@link com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants#PERMISSIONS_REQUIRED}.
     *
     * @param context The {@link Context} in which to verify the permissions.
     * @return boolean true if permitted, otherwise false.
     */
    public static boolean hasPermissions(Context context)
    {
        return Arrays.stream(PERMISSIONS_REQUIRED).allMatch(p -> {
            return ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED;
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (hasPermissions(requireContext()))
        {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment()
            );
        } else
        {
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE)
        {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0])
            {
                Toast.makeText(getContext(), "Permission request granted", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                        PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment()
                );
            } else
            {
                Toast.makeText(getContext(), "Permission request denied", Toast.LENGTH_LONG).show();
            }
        }
    }
}
