package com.appyhigh.pushNotifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.CleverTapAPI
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class MyFirebaseMessagingService : FirebaseMessagingService() {

    var bitmap: Bitmap? = null
    private var pt_id: String? = null

    //    private TemplateType templateType;
    private var pt_title: String? = null
    private var pt_msg: String? = null
    private var pt_msg_summary: String? = null
    private var pt_large_icon: String? = null
    private var pt_big_img: String? = null
    private var pt_title_clr: String? = null
    private var pt_msg_clr: String? = null
    private var pt_bg: String? = null
    private var contentViewSmall: RemoteViews? = null
    private var contentViewRating: RemoteViews? = null
    private var contentViewBig: RemoteViews? = null
    private val smallIcon = 0
    private var pt_dot = 0
    private var pt_meta_clr: String? = null
    private var pt_small_icon_clr: String? = null
    private var pt_dot_sep: Bitmap? = null
    private var pt_subtitle: String? = null
    private val pt_small_view: String? = null
    private var mainActivity: String? = null


    override fun onNewToken(s: String) {
        super.onNewToken(s)
        Log.d("NEW_TOKEN", s)
    }


    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        //
        try {
            Log.d(TAG, "From: " + remoteMessage.from)
            // Check if message contains a data payload.
            if (remoteMessage.data.size > 0) {
                Log.d(TAG, "Message data payload: " + remoteMessage.data)
                // Check if message contains a notification payload.
                if (remoteMessage.notification != null) {
                    Log.d(TAG, "Message Notification Body: " + remoteMessage.notification!!.body)
                }
                val extras = Bundle()
                for ((key, value) in remoteMessage.data) {
                    extras.putString(key, value)
                }
                val notificationType = extras.getString("notificationType")
                val info = CleverTapAPI.getNotificationInfo(extras)
                if (info.fromCleverTap) {
                    val message = extras.getString("pt_msg")
                    if (message != null) {
                        if (message !== "") {
                            when (notificationType) {
                                "R" -> {
                                    setUp(this, extras)
                                    renderRatingNotification(this, extras)
                                }
                                "Z" -> {
                                    setUp(this, extras)
                                    renderZeroBezelNotification(this, extras)
                                }
                            }
                        }
                        else {
                            CleverTapAPI.getDefaultInstance(this)!!.pushNotificationViewedEvent(
                                extras
                            )
                        }
                    }
                }
                else {
                    setUp(this, extras)
                    when (notificationType) {
                        "R" -> {
                            setUp(this, extras)
                            renderRatingNotification(this, extras)
                        }
                        "Z" -> {
                            setUp(this, extras)
                            renderZeroBezelNotification(this, extras)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*
     *To get a Bitmap image from the URL received
     * */
    fun getBitmapfromUrl(imageUrl: String?): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Log.d(TAG, "getBitmapfromUrl: $e")
            null
        }
    }


    private fun renderRatingNotification(context: Context, extras: Bundle) {
        try {
            contentViewRating = RemoteViews(packageName, R.layout.notification)
            setCustomContentViewBasicKeys(contentViewRating!!, context)
            contentViewSmall = RemoteViews(packageName, R.layout.content_view_small)
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

            //Set the rating stars
            contentViewRating!!.setImageViewResource(R.id.star1, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star2, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star3, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star4, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star5, R.drawable.pt_star_outline)

//            setCustomContentViewBigImage(contentViewRating, pt_big_img);
            bitmapImage = getBitmapfromUrl(pt_big_img)
            contentViewRating!!.setImageViewBitmap(R.id.big_image, bitmapImage)
            //            setCustomContentViewLargeIcon(contentViewSmall, pt_large_icon);
            contentViewSmall!!.setImageViewBitmap(R.id.large_icon, bitmapImage)


            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val id = "messenger_general"
            val name = "General"
            val description = "General Notifications sent by the app"
            val rand = Random()
            val a = rand.nextInt(101) + 1

            val notificationIntent1 = Intent(context, PushTemplateReceiver::class.java)
            notificationIntent1.putExtra("clicked", 1)
            notificationIntent1.putExtra("notificationId", a + 1)
            notificationIntent1.putExtras(extras)
            val contentIntent1 = PendingIntent.getBroadcast(
                context,
                Random().nextInt(),
                notificationIntent1,
                0
            )
            contentViewRating!!.setOnClickPendingIntent(R.id.star1, contentIntent1)

            val notificationIntent2 = Intent(context, PushTemplateReceiver::class.java)
            notificationIntent2.putExtra("clicked", 2)
            notificationIntent2.putExtra("notificationId", a + 1)
            notificationIntent2.putExtras(extras)
            val contentIntent2 = PendingIntent.getBroadcast(
                context,
                Random().nextInt(),
                notificationIntent2,
                0
            )
            contentViewRating!!.setOnClickPendingIntent(R.id.star2, contentIntent2)

            val notificationIntent3 = Intent(context, PushTemplateReceiver::class.java)
            notificationIntent3.putExtra("clicked", 3)
            notificationIntent3.putExtra("notificationId", a + 1)
            notificationIntent3.putExtras(extras)
            val contentIntent3 = PendingIntent.getBroadcast(
                context,
                Random().nextInt(),
                notificationIntent3,
                0
            )
            contentViewRating!!.setOnClickPendingIntent(R.id.star3, contentIntent3)

            val notificationIntent4 = Intent(context, PushTemplateReceiver::class.java)
            notificationIntent4.putExtra("clicked", 4)
            notificationIntent4.putExtra("notificationId", a + 1)
            notificationIntent4.putExtras(extras)
            val contentIntent4 = PendingIntent.getBroadcast(
                context,
                Random().nextInt(),
                notificationIntent4,
                0
            )
            contentViewRating!!.setOnClickPendingIntent(R.id.star4, contentIntent4)

            val notificationIntent5 = Intent(context, PushTemplateReceiver::class.java)
            notificationIntent5.putExtra("clicked", 5)
            notificationIntent5.putExtra("notificationId", a + 1)
            notificationIntent5.putExtras(extras)
            val contentIntent5 = PendingIntent.getBroadcast(
                context,
                Random().nextInt(),
                notificationIntent5,
                0
            )
            contentViewRating!!.setOnClickPendingIntent(R.id.star5, contentIntent5)

            val launchIntent = Intent(context, PushTemplateReceiver::class.java)
            val pIntent = setPendingIntent(context, extras, launchIntent)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notificationBuilder = NotificationCompat.Builder(this, id)
                //                    .setLargeIcon(image)/*Notification icon image*/
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(pt_title)
                .setContentText(pt_msg) //                    .setStyle(new NotificationCompat.BigPictureStyle()
                //                            .bigPicture(image))/*Notification with Image*/
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewRating)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pIntent)
                .setPriority(Notification.PRIORITY_DEFAULT)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // The id of the channel.
                val mChannel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
                mChannel.description = description
                mChannel.enableLights(true)
                mChannel.lightColor = Color.BLUE
                mChannel.enableVibration(true)

                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(mChannel)
                    notificationManager.notify(a + 1, notificationBuilder.setChannelId(id).build())
                }
            }
            else {
                notificationManager?.notify(a + 1, notificationBuilder.build())
            }
            Log.d(TAG, "renderRatingNotification: ")

//            Utils.raiseNotificationViewed(context, extras, config);
        }
        catch (t: Throwable) {
            Log.d(TAG, "renderRatingNotification: $t")
        }
    }

    private fun renderZeroBezelNotification(context: Context, extras: Bundle) {
        try {
            contentViewBig = RemoteViews(context.packageName, R.layout.zero_bezel)
            setCustomContentViewBasicKeys(contentViewBig!!, context)
            val textOnlySmallView = pt_small_view != null && pt_small_view == "text_only"
            contentViewSmall = if (textOnlySmallView) {
                RemoteViews(context.packageName, R.layout.cv_small_text_only)
            } else {
                RemoteViews(context.packageName, R.layout.cv_small_zero_bezel)
            }


            setCustomContentViewBasicKeys(contentViewSmall!!, context)
            setCustomContentViewTitle(contentViewBig!!, pt_title)
            setCustomContentViewTitle(contentViewSmall!!, pt_title)
            setCustomContentViewMessage(contentViewBig!!, pt_msg)

            if (textOnlySmallView) {
                contentViewSmall!!.setViewVisibility(R.id.msg, View.GONE)
            } else {
                setCustomContentViewMessage(contentViewSmall!!, pt_msg)
            }

            setCustomContentViewMessageSummary(contentViewBig!!, pt_msg_summary)
            setCustomContentViewTitleColour(contentViewBig!!, pt_title_clr)
            setCustomContentViewTitleColour(contentViewSmall!!, pt_title_clr)
            setCustomContentViewExpandedBackgroundColour(contentViewBig!!, pt_bg)
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)
            setCustomContentViewMessageColour(contentViewBig!!, pt_msg_clr)
            setCustomContentViewMessageColour(contentViewSmall!!, pt_msg_clr)

            val launchIntent = Intent(context, Class.forName(mainActivity))
            launchIntent.putExtras(extras)
            launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            val pIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
            )
            bitmapImage = getBitmapfromUrl(pt_big_img)
            contentViewBig!!.setImageViewBitmap(R.id.big_image, bitmapImage)

            if (!textOnlySmallView) {
                contentViewSmall!!.setImageViewBitmap(R.id.big_image, bitmapImage)
            }
            if (textOnlySmallView) {
                contentViewSmall!!.setImageViewBitmap(R.id.large_icon, bitmapImage)
            }

            contentViewSmall!!.setImageViewResource(
                R.id.small_icon,
                R.drawable.ic_launcher_foreground
            )
            contentViewBig!!.setImageViewResource(
                R.id.small_icon,
                R.drawable.ic_launcher_foreground
            )
//
//            setCustomContentViewDotSep(contentViewBig);
//            setCustomContentViewDotSep(contentViewSmall);
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val id = "messenger_general"
            val name = "General"
            val description = "General Notifications sent by the app"
            val rand = Random()
            val a = rand.nextInt(101) + 1
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this, id)
                //.setLargeIcon(image)/*Notification icon image*/
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(pt_title)
                .setContentText(pt_msg)
                //.setStyle(new NotificationCompat.BigPictureStyle()
                //.bigPicture(image))/*Notification with Image*/
                //.setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewBig)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pIntent)
                .setPriority(Notification.PRIORITY_DEFAULT)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // The id of the channel.
                val mChannel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
                mChannel.description = description
                mChannel.enableLights(true)
                mChannel.lightColor = Color.BLUE
                mChannel.enableVibration(true)
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(mChannel)
                    notificationManager.notify(a + 1, notificationBuilder.setChannelId(id).build())
                }
            }
            else {
                notificationManager?.notify(a + 1, notificationBuilder.build())
            }
            Log.d(TAG, "renderZeroBezelNotification: ")
        } catch (t: Throwable) {
            Log.d(TAG, "renderZeroBezelNotification: $t")
        }
    }




    private fun setPendingIntent(context: Context, extras: Bundle, launchIntent: Intent): PendingIntent {
        launchIntent.putExtras(extras)
        launchIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        return PendingIntent.getBroadcast(
            context, System.currentTimeMillis().toInt(),
            launchIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }


    private fun setCustomContentViewBasicKeys(contentView: RemoteViews, context: Context) {
        contentView.setTextViewText(R.id.app_name, Utils.getApplicationName(context))
        contentView.setTextViewText(R.id.timestamp, Utils.getTimeStamp(context))
        if (pt_subtitle != null && !pt_subtitle!!.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setTextViewText(
                    R.id.subtitle, Html.fromHtml(
                        pt_subtitle,
                        Html.FROM_HTML_MODE_LEGACY
                    )
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


    private fun setDotSep(context: Context) {
        try {
            pt_dot = context.resources.getIdentifier(
                "pt_dot_sep",
                "drawable",
                context.packageName
            )
            pt_dot_sep = Utils.setBitMapColour(context, pt_dot, pt_meta_clr)
        } catch (e: NullPointerException) {
//            PTLog.debug("NPE while setting dot sep color");
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

    companion object {
        private const val TAG = "FirebaseMessageService"
        var bitmapImage: Bitmap? = null
            private set
    }
}