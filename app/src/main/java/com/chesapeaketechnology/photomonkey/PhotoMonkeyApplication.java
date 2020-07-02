package com.chesapeaketechnology.photomonkey;

import android.app.Application;
import android.content.Context;

/**
 * Used to provide a single point for model classes to access the application
 * context. Since the application object is only created on application start,
 * it acts as a quasi-singleton.
 *
 * @since 0.2.0
 */
public class PhotoMonkeyApplication extends Application
{
    private static Application instance;

    /**
     * Get Application level {@link Context}.
     *
     * @return application level {@link Context}
     */
    public static Context getContext()
    {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
    }
}
