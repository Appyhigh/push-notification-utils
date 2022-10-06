package com.appyhigh.pushNotifications.models

import android.graphics.Bitmap

interface BitmapLoadListener {
    fun onSuccess(bitmap: Bitmap?)
}