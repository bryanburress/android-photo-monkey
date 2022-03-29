package com.chesapeaketechnology.photomonkey.util;

import android.content.Context;
import android.content.RestrictionsManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility class related to Android preferences and restrictions.
 *
 * @since 0.2.0
 */
public class PreferenceUtils
{
    public static String getPostEndpointPreference(Context context)
    {
        final RestrictionsManager restrictionsManager = (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);

        final boolean mdmOverride = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PhotoMonkeyConstants.PROPERTY_MDM_OVERRIDE_KEY, false);

        // First try to use the MDM provided value.
        if (restrictionsManager != null && !mdmOverride)
        {
            final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();

            if (mdmProperties.containsKey(PhotoMonkeyConstants.PROPERTY_REMOTE_POST_URL))
            {
                return mdmProperties.getString(PhotoMonkeyConstants.PROPERTY_REMOTE_POST_URL);
            }
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Next, try to use the value from user preferences
        return preferences.getString(PhotoMonkeyConstants.PROPERTY_REMOTE_POST_URL, "");
    }

    public static String getDeviceIDPreference(Context context)
    {
        final RestrictionsManager restrictionsManager = (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);

        final boolean mdmOverride = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PhotoMonkeyConstants.PROPERTY_MDM_OVERRIDE_KEY, false);

        // First try to use the MDM provided value.
        if (restrictionsManager != null && !mdmOverride)
        {
            final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();

            if (mdmProperties.containsKey(PhotoMonkeyConstants.PROPERTY_DEVICE_ID_KEY))
            {
                return mdmProperties.getString(PhotoMonkeyConstants.PROPERTY_DEVICE_ID_KEY);
            }
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Next, try to use the value from user preferences
        return preferences.getString(PhotoMonkeyConstants.PROPERTY_DEVICE_ID_KEY, "");
    }

    public static Boolean getVpnOnlyPreference(Context context)
    {
        final RestrictionsManager restrictionsManager = (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);

        final boolean mdmOverride = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PhotoMonkeyConstants.PROPERTY_MDM_OVERRIDE_KEY, false);

        // First try to use the MDM provided value.
        if (restrictionsManager != null && !mdmOverride)
        {
            final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();

            if (mdmProperties.containsKey(PhotoMonkeyConstants.PROPERTY_VPN_ONLY_KEY))
            {
                return mdmProperties.getBoolean(PhotoMonkeyConstants.PROPERTY_VPN_ONLY_KEY);
            }
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Next, try to use the value from user preferences
        return preferences.getBoolean(PhotoMonkeyConstants.PROPERTY_VPN_ONLY_KEY, false);
    }

    public static Boolean getWifiOnlyPreference(Context context)
    {
        final RestrictionsManager restrictionsManager = (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);

        final boolean mdmOverride = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PhotoMonkeyConstants.PROPERTY_MDM_OVERRIDE_KEY, true);

        // First try to use the MDM provided value.
        if (restrictionsManager != null && !mdmOverride)
        {
            final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();

            if (mdmProperties.containsKey(PhotoMonkeyConstants.PROPERTY_WIFI_ONLY_KEY))
            {
                return mdmProperties.getBoolean(PhotoMonkeyConstants.PROPERTY_WIFI_ONLY_KEY);
            }
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Next, try to use the value from user preferences
        return preferences.getBoolean(PhotoMonkeyConstants.PROPERTY_WIFI_ONLY_KEY, true);
    }

    public static String getBaseUrl(String url)
    {
        Uri remoteUri = Uri.parse(url);
        return remoteUri.getScheme() + "://" + remoteUri.getAuthority() + "/";
    }

    public static String getPathUrl(String url)
    {
        return Uri.parse(url).getPath();
    }

    public static Map<String, String> getQueryParameterMap(String url)
    {
        Uri remote = Uri.parse(url);
        Set<String> params = remote.getQueryParameterNames();
        Map<String, String> queryParameterMap = new HashMap<>();

        params.forEach(param -> {
            queryParameterMap.put(param, remote.getQueryParameter(param));
        });
        return queryParameterMap;
    }
}
