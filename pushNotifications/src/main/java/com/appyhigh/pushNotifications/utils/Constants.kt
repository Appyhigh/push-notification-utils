package com.appyhigh.pushNotifications.utils

import android.app.Activity
import android.app.IntentService
import com.appyhigh.pushNotifications.R

object Constants {
    val FCM_DEBUG_TOPIC = ""
    val FCM_RELASE_TOPIC = ""
    var FCM_TARGET_ACTIVITY: Class<out Activity?>? = null
    var FCM_TARGET_SERVICE: Class<out IntentService?>? = null
    var FCM_ICON :Int = R.drawable.pt_dot_sep
    var ENABLE_NOTIFICATION_GROUPING = "enable_push_lib_grouping"
    var PUSH_LIB_PREFS = "push_lib_prefs"
}