package com.appyhigh.pushNotifications

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.appyhigh.pushNotifications.Constants.FCM_ICON
import com.appyhigh.pushNotifications.Constants.FCM_TARGET_ACTIVITY
import java.util.*
import com.appyhigh.pushNotifications.MyFirebaseMessagingService.Companion.bitmapImage;

class PushTemplateReceiver : BroadcastReceiver() {
    var clicked = 0
    private var contentViewSmall: RemoteViews? = null
    private var contentViewRating: RemoteViews? = null
    private var title: String? = null
    private var message: String? = null
    private var messageBody: String? = null
    private var large_icon: String? = null
    private var title_clr: String? = null
    private var message_clr: String? = null
    private var pt_bg: String? = null
    private var pt_dot = 0
    private var small_icon_clr: String? = null
    private var image: String? = null
    private var meta_clr: String? = null
    private val TAG = "TemplateReciver"


    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ")
        //        Utils.createSilentNotificationChannel(context);
        if (intent.extras != null) {
            val extras = intent.extras
            setUp(context, extras)
            handleRatingNotification(context, extras)
        }
    }

    private fun handleRatingNotification(context: Context, extras: Bundle?) {
        Log.d(TAG, "onReceive:inside ")
        try {

            //Set RemoteViews again
            contentViewRating = RemoteViews(context.packageName, R.layout.notification)
            setCustomContentViewBasicKeys(contentViewRating!!, context)
            contentViewSmall = RemoteViews(context.packageName, R.layout.content_view_small)
            setCustomContentViewBasicKeys(contentViewSmall!!, context)
            setCustomContentViewTitle(contentViewRating!!, title)
            setCustomContentViewTitle(contentViewSmall!!, title)
            setCustomContentViewMessage(contentViewRating!!, message)
            setCustomContentViewMessage(contentViewSmall!!, message)
            setCustomContentViewMessageSummary(contentViewRating!!, messageBody)
            setCustomContentViewTitleColour(contentViewRating!!, title_clr)
            setCustomContentViewTitleColour(contentViewSmall!!, title_clr)
            setCustomContentViewMessageColour(contentViewRating!!, message_clr)
            setCustomContentViewMessageColour(contentViewSmall!!, message_clr)
            setCustomContentViewExpandedBackgroundColour(contentViewRating!!, pt_bg)
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)
            val map = HashMap<String, Any>()
            Log.d(TAG, "handleRatingNotification big image: $image")
            clicked = extras!!.getInt("clicked", 0)
            Log.d(TAG, "handleRatingNotification: $clicked")
            when (clicked) {
                1 -> contentViewRating!!.setImageViewResource(R.id.star1, R.drawable.pt_star_filled)
                2 -> {
                    contentViewRating!!.setImageViewResource(R.id.star1, R.drawable.pt_star_filled)
                    contentViewRating!!.setImageViewResource(R.id.star2, R.drawable.pt_star_filled)
                }
                3 -> {
                    contentViewRating!!.setImageViewResource(R.id.star1, R.drawable.pt_star_filled)
                    contentViewRating!!.setImageViewResource(R.id.star2, R.drawable.pt_star_filled)
                    contentViewRating!!.setImageViewResource(R.id.star3, R.drawable.pt_star_filled)
                }
                4 -> {
                    contentViewRating!!.setImageViewResource(R.id.star1, R.drawable.pt_star_filled)
                    contentViewRating!!.setImageViewResource(R.id.star2, R.drawable.pt_star_filled)
                    contentViewRating!!.setImageViewResource(R.id.star3, R.drawable.pt_star_filled)
                    contentViewRating!!.setImageViewResource(R.id.star4, R.drawable.pt_star_filled)
                }
                5 -> {
                    contentViewRating!!.setImageViewResource(R.id.star1, R.drawable.pt_star_filled)
                    contentViewRating!!.setImageViewResource(R.id.star2, R.drawable.pt_star_filled)
                    contentViewRating!!.setImageViewResource(R.id.star3, R.drawable.pt_star_filled)
                    contentViewRating!!.setImageViewResource(R.id.star4, R.drawable.pt_star_filled)
                    contentViewRating!!.setImageViewResource(R.id.star5, R.drawable.pt_star_filled)
                }
                else -> {
                    contentViewRating!!.setImageViewResource(R.id.star1, R.drawable.pt_star_outline)
                    contentViewRating!!.setImageViewResource(R.id.star2, R.drawable.pt_star_outline)
                    contentViewRating!!.setImageViewResource(R.id.star3, R.drawable.pt_star_outline)
                    contentViewRating!!.setImageViewResource(R.id.star4, R.drawable.pt_star_outline)
                    contentViewRating!!.setImageViewResource(R.id.star5, R.drawable.pt_star_outline)
                }
            }
            contentViewRating!!.setImageViewBitmap(R.id.big_image, bitmapImage)
            contentViewSmall!!.setImageViewBitmap(R.id.large_icon, bitmapImage)
            val id = "messenger_general"
            val name = "General"
            val description = "General Notifications sent by the app"
            val notificationId = extras.getInt("notificationId")
            var launchIntent = Intent(context, FCM_TARGET_ACTIVITY)
            launchIntent.putExtras(extras)
            launchIntent.putExtra("rating", clicked)
            launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            val pIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
            )
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(
                context,
                id
            ) //                    .setLargeIcon(image)/*Notification icon image*/
                .setSmallIcon(FCM_ICON)
                .setContentTitle(title)
                .setContentText(message) //                    .setStyle(new NotificationCompat.BigPictureStyle()
                //                            .bigPicture(image))/*Notification with Image*/
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewRating)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentIntent(pIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // The id of the channel.
                val mChannel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
                mChannel.description = description
                mChannel.enableLights(true)
                mChannel.lightColor = Color.BLUE
                mChannel.enableVibration(true)
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(mChannel)
                    notificationManager.notify(
                        notificationId,
                        notificationBuilder.setChannelId(id).build()
                    )
                }
            } else {
                notificationManager?.notify(notificationId, notificationBuilder.build())
            }
            Thread.sleep(1000)
            notificationManager.cancel(notificationId)
            val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            context.sendBroadcast(it)
            context.startActivity(launchIntent)
        } catch (t: Throwable) {
//            PTLog.verbose("Error creating rating notification ", t);
            Log.d(TAG, "onReceive: $t")
        }
    }



    private fun setCustomContentViewBasicKeys(contentView: RemoteViews, context: Context) {
        contentView.setTextViewText(R.id.app_name, Utils.getApplicationName(context))
        contentView.setTextViewText(R.id.timestamp, Utils.getTimeStamp(context))
        if (meta_clr != null && !meta_clr!!.isEmpty()) {
            contentView.setTextColor(
                R.id.app_name,
                Utils.getColour(meta_clr, "#A6A6A6")
            )
            contentView.setTextColor(
                R.id.timestamp,
                Utils.getColour(meta_clr, "#A6A6A6")
            )
            contentView.setTextColor(
                R.id.subtitle,
                Utils.getColour(meta_clr, "#A6A6A6")
            )
            setDotSep(context)
        }
    }

    private fun setCustomContentViewMessageSummary(
        contentView: RemoteViews,
        messageBody: String?
    ) {
        if (messageBody != null && !messageBody.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(
                    R.id.msg,
                    Html.fromHtml(messageBody, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(messageBody))
            }
        }
    }

    private fun setCustomContentViewMessageColour(contentView: RemoteViews, message_clr: String?) {
        if (message_clr != null && !message_clr.isEmpty()) {
            contentView.setTextColor(
                R.id.msg,
                Utils.getColour(message_clr, "#000000")
            )
        }
    }

    private fun setCustomContentViewTitleColour(contentView: RemoteViews, title_clr: String?) {
        if (title_clr != null && !title_clr.isEmpty()) {
            contentView.setTextColor(
                R.id.title,
                Utils.getColour(title_clr, "#000000")
            )
        }
    }

    private fun setCustomContentViewExpandedBackgroundColour(
        contentView: RemoteViews,
        pt_bg: String?
    ) {
        if (pt_bg != null && !pt_bg.isEmpty()) {
            contentView.setInt(
                R.id.content_view_big,
                "setBackgroundColor",
                Utils.getColour(pt_bg, "#FFFFFF")
            )
        }
    }

    private fun setCustomContentViewCollapsedBackgroundColour(
        contentView: RemoteViews,
        pt_bg: String?
    ) {
        if (pt_bg != null && !pt_bg.isEmpty()) {
            contentView.setInt(
                R.id.content_view_small,
                "setBackgroundColor",
                Utils.getColour(pt_bg, "#FFFFFF")
            )
        }
    }

    private fun setCustomContentViewMessage(contentView: RemoteViews, message: String?) {
        if (message != null && !message.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(
                    R.id.msg,
                    Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(message))
            }
        }
    }

    private fun setCustomContentViewTitle(contentView: RemoteViews, title: String?) {
        if (title != null && !title.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(
                    R.id.title,
                    Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                contentView.setTextViewText(R.id.title, Html.fromHtml(title))
            }
        }
    }

    private fun setDotSep(context: Context) {
        try {
            pt_dot = context.resources.getIdentifier(
                "dot_sep",
                "drawable",
                context.packageName
            )
            //            dot_sep = Utils.setBitMapColour(context, pt_dot, meta_clr);
        } catch (e: NullPointerException) {
//            PTLog.debug("NPE while setting dot sep color");
        }
    }

    private fun setUp(context: Context, extras: Bundle?) {
        message = extras!!.getString("message")
        messageBody = extras!!.getString("messageBody")
        message_clr = extras!!.getString("message_clr")
        title = extras!!.getString("title")
        if(message==null || message.equals("")){
            message = extras!!.getString("nm")
        }
        if(title==null || title.equals("")){
            title = extras.getString("nt")
        }
        title_clr = extras!!.getString("title_clr")
        meta_clr = extras!!.getString("meta_clr")
        pt_bg = extras!!.getString("pt_bg")
        image = extras!!.getString("image")
        large_icon = extras!!.getString("large_icon")
        small_icon_clr = extras!!.getString("small_icon_clr")
    }
}