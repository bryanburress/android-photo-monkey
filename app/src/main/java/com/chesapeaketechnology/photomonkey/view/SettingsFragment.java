package com.chesapeaketechnology.photomonkey.view;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PROPERTY_DEVICE_ID_KEY;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PROPERTY_MDM_OVERRIDE_KEY;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PROPERTY_REMOTE_POST_URL;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PROPERTY_VPN_ONLY_KEY;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PROPERTY_WIFI_ONLY_KEY;

import android.content.Context;
import android.content.RestrictionsManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;


import com.chesapeaketechnology.photomonkey.R;
import com.chesapeaketechnology.photomonkey.util.MdmUtils;

import java.util.Objects;

import timber.log.Timber;

/**
 * A Settings Fragment to inflate the Preferences XML resource so the user can interact with the App's settings.
 *
 * @since 0.2.0
 */
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener
{
    /**
     * The list of preferences that can be set in both the MDM app restrictions, and this settings UI.
     */
    private static final String[] PROPERTY_KEYS = {PROPERTY_REMOTE_POST_URL,
            PROPERTY_DEVICE_ID_KEY,
            PROPERTY_VPN_ONLY_KEY,
            PROPERTY_WIFI_ONLY_KEY};

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        getContext().getTheme().applyStyle(R.style.AlertDialogTheme, true);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        // Inflate the preferences XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).registerOnSharedPreferenceChangeListener(this);

        EditTextPreference deviceIdPreference = findPreference(PROPERTY_DEVICE_ID_KEY);
        if (deviceIdPreference != null)
        {
            deviceIdPreference.setOnBindEditTextListener(
                    editText -> editText.setTextColor(getResources().getColor(R.color.primaryDarkColor))
            );
        }

        EditTextPreference urlPreference = findPreference(PROPERTY_REMOTE_POST_URL);
        if (urlPreference != null)
        {
            urlPreference.setOnBindEditTextListener(
                    editText -> editText.setTextColor(getResources().getColor(R.color.primaryDarkColor))
            );
        }

        updateUiForMdmIfNecessary();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        final SharedPreferences.Editor edit = sharedPreferences.edit();

        switch (key)
        {
            case PROPERTY_MDM_OVERRIDE_KEY:
                final boolean mdmOverride = sharedPreferences.getBoolean(key, false);

                Timber.d("mdmOverride Preference Changed to %s", mdmOverride);

                if (mdmOverride)
                {
                    final PreferenceScreen preferenceScreen = getPreferenceScreen();
                    for (String preferenceKey : PROPERTY_KEYS)
                    {
                        final Preference preference = preferenceScreen.findPreference(preferenceKey);
                        if (preference != null) preference.setEnabled(true);
                    }
                } else
                {
                    updateUiForMdmIfNecessary();
                }
                break;

            case PROPERTY_DEVICE_ID_KEY:
                edit.putString(key, sharedPreferences.getString(key, "Not set"));
                edit.apply();
                break;

            case PROPERTY_REMOTE_POST_URL:
                String urlPreference = sharedPreferences.getString(key, "");
                if (urlPreference.matches("https://(.*)"))
                {
                    edit.putString(key, urlPreference);
                } else
                {
                    edit.putString(key, "Not set");
                    Toast.makeText(getContext(), "URL must start with https://", Toast.LENGTH_LONG).show();
                }

                edit.apply();
                break;

            case PROPERTY_VPN_ONLY_KEY:
            case PROPERTY_WIFI_ONLY_KEY:
                edit.putBoolean(key, sharedPreferences.getBoolean(key, false));
                edit.apply();
                break;
            default:
                Timber.wtf("Unknown preference key %s", key);
        }
    }

    @Override
    public void onDestroyView()
    {
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroyView();
    }

    /**
     * If the app is under MDM control, update the user preferences UI to reflect those MDM provided values. If the app
     * is not under MDM control, then do nothing.
     * <p>
     * Also, we need to check if the user has turned on the MDM override option. If so, then the values can
     * still be changed. If not, then we should disable all settings but still update the values so that the UI reflects
     * the MDM provided values.
     *
     * @since 0.4.0
     */
    private void updateUiForMdmIfNecessary()
    {
        if (!MdmUtils.isUnderMdmControl(requireContext(), PROPERTY_KEYS)) return;

        final SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();

        // Update the UI so that the MDM override is visible, and that some of the settings can't be changed
        final Preference overridePreference = getPreferenceScreen().findPreference(PROPERTY_MDM_OVERRIDE_KEY);
        if (overridePreference != null) overridePreference.setVisible(true);

        final boolean mdmOverride = sharedPreferences.getBoolean(PROPERTY_MDM_OVERRIDE_KEY, false);

        if (mdmOverride) return; // Nothing to do because all the preferences are enabled by default.

        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        final RestrictionsManager restrictionsManager = (RestrictionsManager) requireContext().getSystemService(Context.RESTRICTIONS_SERVICE);
        if (restrictionsManager == null) return;

        final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();
        if (mdmProperties == null) return;

        updateBooleanPreferenceForMdm(preferenceScreen, mdmProperties, PROPERTY_VPN_ONLY_KEY);
        updateBooleanPreferenceForMdm(preferenceScreen, mdmProperties, PROPERTY_WIFI_ONLY_KEY);

        updateStringPreferenceForMdm(preferenceScreen, mdmProperties, PROPERTY_REMOTE_POST_URL);
        updateStringPreferenceForMdm(preferenceScreen, mdmProperties, PROPERTY_DEVICE_ID_KEY);
    }

    /**
     * Updates a boolean preference with an MDM value, if it exists. The shared preferences are
     * also updated, so that values are retained when MDM control is off.
     *
     * @param preferenceScreen The preference screen
     * @param mdmProperties    The map of mdm provided properties.
     * @param preferenceKey    The preference key
     * @since 0.4.0
     */
    private void updateBooleanPreferenceForMdm(PreferenceScreen preferenceScreen, Bundle mdmProperties, String preferenceKey)
    {
        try
        {
            final SwitchPreferenceCompat preference = preferenceScreen.findPreference(preferenceKey);

            if (preference != null && mdmProperties.containsKey(preferenceKey))
            {
                final boolean mdmBooleanProperty = mdmProperties.getBoolean(preferenceKey);

                preference.setEnabled(false);
                preference.setChecked(mdmBooleanProperty);

                getPreferenceManager().getSharedPreferences()
                        .edit()
                        .putBoolean(preferenceKey, mdmBooleanProperty)
                        .apply();
            }
        } catch (Exception e)
        {
            Timber.wtf(e, "Could not find the bool preferences or update the UI component for %s", preferenceKey);
        }
    }

    private void updateStringPreferenceForMdm(PreferenceScreen preferenceScreen, Bundle mdmProperties, String preferenceKey)
    {
        try
        {
            final EditTextPreference preference = preferenceScreen.findPreference(preferenceKey);

            if (preference != null && mdmProperties.containsKey(preferenceKey))
            {
                final String mdmStringProperty = mdmProperties.getString(preferenceKey, "");

                if (!mdmStringProperty.isEmpty())
                {
                    preference.setEnabled(false);

                    preference.setSummaryProvider(pref -> mdmStringProperty);

                    getPreferenceManager().getSharedPreferences()
                            .edit()
                            .putString(preferenceKey, String.valueOf(mdmStringProperty))
                            .apply();
                }
            }
        } catch (Exception e)
        {
            Timber.wtf(e, "Could not find the string preference or update the UI component for %s", preferenceKey);
        }
    }
}
