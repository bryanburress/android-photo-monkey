package com.chesapeaketechnology.photomonkey.view;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PROPERTY_REMOTE_POST_URL;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.chesapeaketechnology.photomonkey.R;


/**
 * Fragment responsible for QR code scanning. Leverages an open source code scanning library from
 * Yuriy Budiev.
 *
 * @since 0.2.5
 */
public class CodeScannerFragment extends Fragment
{
    private CodeScanner codeScanner;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        final Activity activity = getActivity();
        View root = inflater.inflate(R.layout.fragment_scanner, container, false);
        CodeScannerView scannerView = root.findViewById(R.id.scanner_view);
        codeScanner = new CodeScanner(activity, scannerView);
        codeScanner.setDecodeCallback(result -> activity.runOnUiThread(() -> {
                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    preferences.edit().putString(PROPERTY_REMOTE_POST_URL, result.getText()).apply();

                    // clear out the backstack for the code scanning fragment
                    getParentFragmentManager().popBackStack();
                    getParentFragmentManager().popBackStack();
                    getParentFragmentManager().popBackStack();

                    Navigation.findNavController(requireActivity(), R.id.fragment_container)
                            .navigate(CodeScannerFragmentDirections.actionCodeScannerFragmentToSettingsFragment(result.getText()));

                }));
        scannerView.setOnClickListener(view -> codeScanner.startPreview());
        return root;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        codeScanner.startPreview();
    }

    @Override
    public void onPause()
    {
        codeScanner.releaseResources();
        super.onPause();
    }
}