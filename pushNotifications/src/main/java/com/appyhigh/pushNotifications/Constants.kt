package com.appyhigh.pushNotifications

import android.app.Activity

object Constants {
    val FCM_DEBUG_TOPIC = ""
    val FCM_RELASE_TOPIC = ""
    var FCM_TARGET_ACTIVITY: Class<out Activity?>? = null
    var FCM_ICON :Int = R.drawable.pt_dot_sep
}