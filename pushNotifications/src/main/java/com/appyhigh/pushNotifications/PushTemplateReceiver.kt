package com.appyhigh.pushNotifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import java.util.*
import com.appyhigh.pushNotifications.MyFirebaseMessagingService.Companion.bitmapImage;

class PushTemplateReceiver : BroadcastReceiver() {
    private var mainActivity: String? = null
    var clicked = 0
    private var contentViewSmall: RemoteViews? = null
    private var contentViewRating: RemoteViews? = null
    private var pt_id: String? = null
    private var pt_title: String? = null
    private var pt_msg: String? = null
    private var pt_msg_summary: String? = null
    private val pt_img_small: String? = null
    private var pt_large_icon: String? = null
    private var pt_title_clr: String? = null
    private var pt_msg_clr: String? = null
    private var deepLinkList = ArrayList<String?>()
    private var pt_bg: String? = null
    private val smallIcon = 0
    private var pt_dot = 0
    private var pt_small_icon_clr: String? = null
    private var pt_big_img: String? = null
    private var pt_meta_clr: String? = null
    private val pt_dot_sep: String? = null
    private var pt_subtitle: String? = null
    private val pID: String? = null
    private val TAG = "TemplateReciver"


    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ")
        //        Utils.createSilentNotificationChannel(context);
        if (intent.extras != null) {
            val extras = intent.extras
            deepLinkList = Utils.getDeepLinkListFromExtras(extras!!)
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
            setCustomContentViewTitle(contentViewRating!!, pt_title)
            setCustomContentViewTitle(contentViewSmall!!, pt_title)
            setCustomContentViewMessage(contentViewRating!!, pt_msg)
            setCustomContentViewMessage(contentViewSmall!!, pt_msg)
            setCustomContentViewMessageSummary(contentViewRating!!, pt_msg_summary)
            setCustomContentViewTitleColour(contentViewRating!!, pt_title_clr)
            setCustomContentViewTitleColour(contentViewSmall!!, pt_title_clr)
            setCustomContentViewMessageColour(contentViewRating!!, pt_msg_clr)
            setCustomContentViewMessageColour(contentViewSmall!!, pt_msg_clr)
            setCustomContentViewExpandedBackgroundColour(contentViewRating!!, pt_bg)
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)
            val map = HashMap<String, Any>()
            Log.d(TAG, "handleRatingNotification big image: $pt_big_img")
            clicked = extras!!.getInt("clicked", 0)
            val pt_dl_clicked = deepLinkList[4]
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
            var launchIntent = Intent()
            if (clicked == 5 && !pt_dl_clicked.equals("")) {
                launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(pt_dl_clicked))
            } else {
                launchIntent = Intent(context, Class.forName(mainActivity))
            }
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
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(pt_title)
                .setContentText(pt_msg) //                    .setStyle(new NotificationCompat.BigPictureStyle()
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
        if (pt_subtitle != null && !pt_subtitle!!.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(
                    R.id.subtitle,
                    Html.fromHtml(pt_subtitle, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                contentView.setTextViewText(R.id.subtitle, Html.fromHtml(pt_subtitle))
            }
        } else {
            contentView.setViewVisibility(R.id.subtitle, View.GONE)
            contentView.setViewVisibility(R.id.sep_subtitle, View.GONE)
        }
        if (pt_meta_clr != null && !pt_meta_clr!!.isEmpty()) {
            contentView.setTextColor(
                R.id.app_name,
                Utils.getColour(pt_meta_clr, "#A6A6A6")
            )
            contentView.setTextColor(
                R.id.timestamp,
                Utils.getColour(pt_meta_clr, "#A6A6A6")
            )
            contentView.setTextColor(
                R.id.subtitle,
                Utils.getColour(pt_meta_clr, "#A6A6A6")
            )
            setDotSep(context)
        }
    }

    private fun setCustomContentViewMessageSummary(
        contentView: RemoteViews,
        pt_msg_summary: String?
    ) {
        if (pt_msg_summary != null && !pt_msg_summary.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(
                    R.id.msg,
                    Html.fromHtml(pt_msg_summary, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg_summary))
            }
        }
    }

    private fun setCustomContentViewMessageColour(contentView: RemoteViews, pt_msg_clr: String?) {
        if (pt_msg_clr != null && !pt_msg_clr.isEmpty()) {
            contentView.setTextColor(
                R.id.msg,
                Utils.getColour(pt_msg_clr, "#000000")
            )
        }
    }

    private fun setCustomContentViewTitleColour(contentView: RemoteViews, pt_title_clr: String?) {
        if (pt_title_clr != null && !pt_title_clr.isEmpty()) {
            contentView.setTextColor(
                R.id.title,
                Utils.getColour(pt_title_clr, "#000000")
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

    private fun setCustomContentViewMessage(contentView: RemoteViews, pt_msg: String?) {
        if (pt_msg != null && !pt_msg.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(
                    R.id.msg,
                    Html.fromHtml(pt_msg, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                contentView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg))
            }
        }
    }

    private fun setCustomContentViewTitle(contentView: RemoteViews, pt_title: String?) {
        if (pt_title != null && !pt_title.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(
                    R.id.title,
                    Html.fromHtml(pt_title, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                contentView.setTextViewText(R.id.title, Html.fromHtml(pt_title))
            }
        }
    }

    private fun setDotSep(context: Context) {
        try {
            pt_dot = context.resources.getIdentifier(
                "pt_dot_sep",
                "drawable",
                context.packageName
            )
            //            pt_dot_sep = Utils.setBitMapColour(context, pt_dot, pt_meta_clr);
        } catch (e: NullPointerException) {
//            PTLog.debug("NPE while setting dot sep color");
        }
    }

    private fun setUp(context: Context, extras: Bundle?) {
        pt_id = extras!!.getString("pt_id")
        pt_msg = extras!!.getString("pt_msg")
        pt_msg_summary = extras!!.getString("pt_msg_summary")
        pt_msg_clr = extras!!.getString("pt_msg_clr")
        pt_title = extras!!.getString("pt_title")
        pt_title_clr = extras!!.getString("pt_title_clr")
        pt_meta_clr = extras!!.getString("pt_meta_clr")
        pt_bg = extras!!.getString("pt_bg")
        pt_big_img = extras!!.getString("pt_big_img")
        pt_large_icon = extras!!.getString("pt_large_icon")
        pt_small_icon_clr = extras!!.getString("pt_small_icon_clr")
        pt_subtitle = extras!!.getString("pt_subtitle")
        mainActivity = extras!!.getString("mainActivity")
    }
}