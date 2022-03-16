package com.chesapeaketechnology.photomonkey.view;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.chesapeaketechnology.photomonkey.R;

import java.util.Arrays;
import java.util.Objects;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PERMISSIONS_REQUEST_CODE;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PERMISSIONS_REQUIRED;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PROPERTY_DEVICE_ID_KEY;

import timber.log.Timber;

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
        return Arrays.stream(PERMISSIONS_REQUIRED).allMatch(p -> ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onResume()
    {
        super.onResume();

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
                initializeDeviceId();
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                        PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment()
                );
            } else
            {
                Toast.makeText(getContext(), "Permission request denied", Toast.LENGTH_LONG).show();
            }
        }
    }


    /**
     * Get the device ID, and place it in the shared preferences.
     */
    @SuppressLint("ApplySharedPref")
    private void initializeDeviceId()
    {
        final String deviceIdPreference = Objects.requireNonNull(getActivity())
                .getPreferences(MODE_PRIVATE)
                .getString(PROPERTY_DEVICE_ID_KEY, "");
        if (deviceIdPreference != null && !deviceIdPreference.isEmpty())
        {
            Timber.i("The Device ID is already present in the Shared Preferences, skipping setting it to the App's default ID.");
            return;
        }

        final String deviceId = getDeviceId();
        PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getContext()))
                .edit()
                .putString(PROPERTY_DEVICE_ID_KEY, deviceId)
                .apply();
    }

    /**
     * Attempts to get the device's IMEI if the user has granted the permission.  If not, then a default ID it used.
     *
     * @return The IMEI if it can be found, otherwise the Android ID.
     */
    @SuppressWarnings("ConstantConditions")
    @SuppressLint({"HardwareIds", "MissingPermission"})
    private String getDeviceId()
    {
        String deviceId = null;
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && getContext().getSystemService(Context.TELEPHONY_SERVICE) != null
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) // As of Android API level 29 the IMEI permission is restricted to system apps only.
        {
            TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            deviceId = telephonyManager.getImei();
        }

        // Fall back on the ANDROID_ID
        if (deviceId == null)
        {
            Timber.w("Could not get the device IMEI");
            //Toast.makeText(getApplicationContext(), "Could not get the device IMEI", Toast.LENGTH_SHORT).show();
            deviceId = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        return deviceId;
    }
}
