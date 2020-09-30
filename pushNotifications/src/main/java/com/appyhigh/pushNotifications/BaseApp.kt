package com.appyhigh.pushNotifications

import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import com.appyhigh.pushNotifications.Constants.FCM_DEBUG_TOPIC
import com.appyhigh.pushNotifications.Constants.FCM_RELASE_TOPIC
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

open class BaseApp : Application() {
    override fun registerComponentCallbacks(callback: ComponentCallbacks?) {
        super.registerComponentCallbacks(callback)
    }

    override fun unregisterComponentCallbacks(callback: ComponentCallbacks?) {
        super.unregisterComponentCallbacks(callback)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
//        FirebaseAnalytics.getInstance(this)
        if (BuildConfig.DEBUG)
            FirebaseMessaging.getInstance().subscribeToTopic(FCM_DEBUG_TOPIC)
        else
            FirebaseMessaging.getInstance().subscribeToTopic(FCM_RELASE_TOPIC)

    }

    override fun onTerminate() {
        super.onTerminate()
    }

    override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks?) {
        super.registerActivityLifecycleCallbacks(callback)
    }

    override fun unregisterActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks?) {
        super.unregisterActivityLifecycleCallbacks(callback)
    }

    override fun registerOnProvideAssistDataListener(callback: OnProvideAssistDataListener?) {
        super.registerOnProvideAssistDataListener(callback)
    }

    override fun unregisterOnProvideAssistDataListener(callback: OnProvideAssistDataListener?) {
        super.unregisterOnProvideAssistDataListener(callback)
    }
}