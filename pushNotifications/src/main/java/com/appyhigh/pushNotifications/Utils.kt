package com.appyhigh.pushNotifications

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.util.*


object Utils {

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun getDeepLinkListFromExtras(extras: Bundle): ArrayList<String?> {
        val dlList = ArrayList<String?>()
        for (key in extras.keySet()) {
            if (key.contains("pt_dl")) {
                dlList.add(extras.getString(key))
            }
        }
        return dlList
    }

    fun getTimeStamp(context: Context?): String {
        return DateUtils.formatDateTime(
            context, System.currentTimeMillis(),
            DateUtils.FORMAT_SHOW_TIME
        )
    }


    @Throws(NullPointerException::class)
    fun setBitMapColour(context: Context?, resourceID: Int, clr: String?): Bitmap? {
        if (clr != null && !clr.isEmpty()) {
            val color: Int = getColour(clr, "#A6A6A6")
            val mDrawable = ContextCompat.getDrawable(context!!, resourceID)!!.mutate()
            mDrawable?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            return drawableToBitmap(mDrawable)
        }
        return null
    }

    fun getApplicationName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(
            stringId
        )
    }

    fun getColour(clr: String?, default_clr: String?): Int {
        return try {
            Color.parseColor(clr)
        } catch (e: Exception) {
            //            PTLog.debug("Can not parse colour value: " + clr + " Switching to default colour: " + default_clr);
            Color.parseColor(default_clr)
        }
    }
}
