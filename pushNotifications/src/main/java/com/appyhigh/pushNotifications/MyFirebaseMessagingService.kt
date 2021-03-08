package com.appyhigh.pushNotifications

import android.app.*
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.util.Patterns
import android.view.View
import android.webkit.URLUtil
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.appyhigh.pushNotifications.Constants.FCM_ICON
import com.appyhigh.pushNotifications.Constants.FCM_TARGET_ACTIVITY
import com.appyhigh.pushNotifications.apiclient.APIClient
import com.appyhigh.pushNotifications.apiclient.APIInterface
import com.appyhigh.pushNotifications.models.NotificationPayloadModel
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.InAppNotificationButtonListener
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.json.JSONObject
import retrofit2.Retrofit
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*


class MyFirebaseMessagingService : FirebaseMessagingService(),InAppNotificationButtonListener {

    var bitmap: Bitmap? = null
    private var title: String? = null
    private var message: String? = null
    private var messageBody: String? = null
    private var large_icon: String? = null
    private var image: String? = null
    private var title_clr: String? = null
    private var message_clr: String? = null
    private var pt_bg: String? = null
    private var contentViewSmall: RemoteViews? = null
    private var contentViewRating: RemoteViews? = null
    private var contentViewBig: RemoteViews? = null
    private var pt_dot = 0
    private var meta_clr: String? = null
    private var small_icon_clr: String? = null
    private var dot_sep: Bitmap? = null
    private val small_view: String? = null
    private lateinit var inAppContext: Context
    private var inAppWebViewActivityToOpen: Class<out Activity?>? = null
    private var inAppActivityToOpen: Class<out Activity?>? = null
    private lateinit var inAppIntentParam: String
    private lateinit var appName: String
    private var retrofit: Retrofit? = null
    private var apiInterface: APIInterface? = null
    private var notificationListObservable : Observable<ArrayList<NotificationPayloadModel>>? = null

    fun addTopics(context: Context, appName: String, isDebug: Boolean){
        if(appName.equals("")){
            getAppName(context)
        }
        else {
            this.appName = appName
        }
        if(isDebug){
            firebaseSubscribeToTopic(appName + "Debug")
        }
        else{
            firebaseSubscribeToTopic(appName)
            firebaseSubscribeToTopic(
                appName + "-" + Locale.getDefault().getCountry() + "-" + Locale.getDefault()
                    .getLanguage()
            )
        }
    }

    override fun onMessageSent(s: String) {
        super.onMessageSent(s)
        Log.d(TAG, "onMessageSent: $s")
    }

    override fun onSendError(s: String, e: Exception) {
        super.onSendError(s, e)
        Log.d(TAG, "onSendError: $e")
    }

    fun getAppName(context: Context) {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        if (stringId == 0) {
            appName = applicationInfo.nonLocalizedLabel.toString()
        }
        else {
            appName = context.getString(stringId)
        }
        appName = appName.replace("\\s".toRegex(), "")
        appName = appName.toLowerCase()
    }

    fun firebaseSubscribeToTopic(appName: String){
        try{
            FirebaseMessaging.getInstance().subscribeToTopic(appName)
                .addOnCompleteListener { task ->
                    var msg = "subscribed to $appName"
                    if (!task.isSuccessful) {
                        msg = "not subscribed to $appName"
                    }

                    Log.d(TAG, msg)
                }
        }catch (e: Exception){
            e.printStackTrace()
            Log.d(TAG, "firebaseSubscribeToTopic: " + e)
        }
    }


    override fun onNewToken(s: String) {
        super.onNewToken(s)
        CleverTapAPI.getDefaultInstance(applicationContext)?.pushFcmRegistrationId(s, true)
        Log.d(TAG, "onNewToken: $s")
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
                val sharedPreferences = getSharedPreferences("missedNotifications", MODE_PRIVATE)
                sharedPreferences.edit().putString(
                    extras.getString("link", "default"),
                    extras.toString()
                ).apply()
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).apply {
                    // setting the small icon for notification
                    if(metaData.containsKey("FCM_ICON")){
                        Log.d(TAG, "onMessageReceived: " + metaData.get("FCM_ICON"))
                        FCM_ICON = metaData.getInt("FCM_ICON")
                    }
                    //getting and setting the target activity that is to be opened on notification click
                    if(extras.containsKey("target_activity")){
                        FCM_TARGET_ACTIVITY = Class.forName(extras.getString("target_activity", "")) as Class<out Activity?>?
                    }
                    else if(FCM_TARGET_ACTIVITY == null) {
                        Log.d(TAG, "onMessageReceived: " + metaData.get("FCM_TARGET_ACTIVITY"))
                        FCM_TARGET_ACTIVITY = Class.forName(
                            metaData.get("FCM_TARGET_ACTIVITY").toString()
                        ) as Class<out Activity?>?
                    }
                }
                val info = CleverTapAPI.getNotificationInfo(extras)
                if (info.fromCleverTap) {
                    if (extras.getString("nm") != "" || extras.getString("nm") != null
                    ) {
                        val message = extras.getString("message")
                        if (message != null) {
                            if (message != "") {
                                when (notificationType) {
                                    "R" -> {
                                        setUp(this, extras)
                                        renderRatingNotification(this, extras)
                                    }
                                    "Z" -> {
                                        setUp(this, extras)
                                        renderZeroBezelNotification(this, extras)
                                    }
                                    "O" -> {
                                        setUp(this, extras)
                                        renderOneBezelNotification(this, extras)
                                    }
                                    else -> {
                                        sendNotification(this, extras)
                                    }
                                }
                            } else {
                                CleverTapAPI.getDefaultInstance(this)!!.pushNotificationViewedEvent(
                                    extras
                                )
                            }
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
                        "O" -> {
                            setUp(this, extras)
                            renderOneBezelNotification(this, extras)
                        }
                        else -> {
                            Log.d(TAG, "onMessageReceived: in else part")
                            sendNotification(this, extras)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun sendNotification(context: Context, extras: Bundle) {
        try {
            var message = extras.getString("message")
            var image = getBitmapfromUrl(extras.getString("image"))
            var url = extras.getString("link")
            var which = extras.getString("which")
            var title = extras.getString("title")
            if(message==null || message.equals("")){
                message = extras!!.getString("nm")
            }
            if(title==null || title.equals("")){
                title = extras.getString("nt")
            }
            Log.i("Result", "Got the data yessss")
            val rand = Random()
            val a = rand.nextInt(101) + 1
            val intent = Intent(context.applicationContext, FCM_TARGET_ACTIVITY)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("link", url)
            intent.putExtras(extras);
            intent.action = java.lang.Long.toString(System.currentTimeMillis())
            val pendingIntent = PendingIntent.getActivity(
                context.applicationContext,
                0 /* Request code */,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
            )
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if(title!=null && message!=null) {
                var notificationBuilder: NotificationCompat.Builder;
                if (image == null || image.equals("")) {
                    notificationBuilder = NotificationCompat.Builder(context.applicationContext)
                        .setSmallIcon(FCM_ICON)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(Notification.PRIORITY_DEFAULT)
                } else {
                    notificationBuilder = NotificationCompat.Builder(context.applicationContext)
                        .setLargeIcon(image) /*Notification icon image*/
                        .setSmallIcon(FCM_ICON)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(image)
                        ) /*Notification with Image*/
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(Notification.PRIORITY_DEFAULT)
                }
                val notificationManager =
                    context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // The id of the channel.
                    val id = "messenger_general"
                    val name: CharSequence = "General"
                    val description = "General Notifications sent by the app"
                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val mChannel = NotificationChannel(id, name, importance)
                    mChannel.description = description
                    mChannel.enableLights(true)
                    mChannel.lightColor = Color.BLUE
                    mChannel.enableVibration(true)
                    notificationManager.createNotificationChannel(mChannel)
                    notificationManager.notify(a + 1, notificationBuilder.setChannelId(id).build())
                } else {
                    notificationManager.notify(
                        a + 1 /* ID of notification */,
                        notificationBuilder.build()
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun renderRatingNotification(context: Context, extras: Bundle) {
        try {
            contentViewRating = RemoteViews(packageName, R.layout.notification)
            setCustomContentViewBasicKeys(contentViewRating!!, context)
            contentViewSmall = RemoteViews(packageName, R.layout.content_view_small)
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

            //Set the rating stars
            contentViewRating!!.setImageViewResource(R.id.star1, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star2, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star3, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star4, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star5, R.drawable.pt_star_outline)


//            setCustomContentViewBigImage(contentViewRating, image);
            bitmapImage = getBitmapfromUrl(image)
            if(bitmapImage!=null) {
                contentViewRating!!.setImageViewBitmap(R.id.big_image, bitmapImage)
                //            setCustomContentViewLargeIcon(contentViewSmall, large_icon);
                contentViewSmall!!.setImageViewBitmap(R.id.large_icon, bitmapImage)
            }


            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
                .setSmallIcon(FCM_ICON)
                .setContentTitle(title)
                .setContentText(message) //                    .setStyle(new NotificationCompat.BigPictureStyle()
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
            contentViewSmall = RemoteViews(context.packageName, R.layout.cv_small_zero_bezel)


            setCustomContentViewBasicKeys(contentViewSmall!!, context)
            setCustomContentViewTitle(contentViewBig!!, title)
            setCustomContentViewTitle(contentViewSmall!!, title)
            setCustomContentViewMessage(contentViewBig!!, message)

            setCustomContentViewMessage(contentViewSmall!!, message)


            setCustomContentViewMessageSummary(contentViewBig!!, messageBody)
            setCustomContentViewTitleColour(contentViewBig!!, title_clr)
            setCustomContentViewTitleColour(contentViewSmall!!, title_clr)
            setCustomContentViewExpandedBackgroundColour(contentViewBig!!, pt_bg)
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)
            setCustomContentViewMessageColour(contentViewBig!!, message_clr)
            setCustomContentViewMessageColour(contentViewSmall!!, message_clr)

            val launchIntent = Intent(context, FCM_TARGET_ACTIVITY)
            launchIntent.putExtras(extras)
            launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            launchIntent.action = java.lang.Long.toString(System.currentTimeMillis())
            val pIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
            )
            bitmapImage = getBitmapfromUrl(image)
            if(bitmapImage!=null) {
                contentViewBig!!.setImageViewBitmap(R.id.big_image, bitmapImage)

                contentViewSmall!!.setImageViewBitmap(R.id.big_image, bitmapImage)
            }

            contentViewSmall!!.setImageViewResource(
                R.id.small_icon,
                FCM_ICON
            )
            contentViewBig!!.setImageViewResource(
                R.id.small_icon,
                FCM_ICON
            )
//
//            setCustomContentViewDotSep(contentViewBig);
//            setCustomContentViewDotSep(contentViewSmall);
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val id = "messenger_general"
            val name = "General"
            val description = "General Notifications sent by the app"
            val rand = Random()
            val a = rand.nextInt(101) + 1
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this, id)
                //.setLargeIcon(image)/*Notification icon image*/
                .setSmallIcon(FCM_ICON)
                .setContentTitle(title)
                .setContentText(message)
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



    private fun renderOneBezelNotification(context: Context, extras: Bundle) {
        try {
            contentViewBig = RemoteViews(context.packageName, R.layout.one_bezel)
            setCustomContentViewBasicKeys(contentViewBig!!, context)
            contentViewSmall = RemoteViews(context.packageName, R.layout.cv_small_one_bezel)

            setCustomContentViewBasicKeys(contentViewSmall!!, context)
            setCustomContentViewTitle(contentViewBig!!, title)
            setCustomContentViewTitle(contentViewSmall!!, title)
            setCustomContentViewMessage(contentViewBig!!, message)
            setCustomContentViewMessage(contentViewSmall!!, message)


            setCustomContentViewMessageSummary(contentViewBig!!, messageBody)
            setCustomContentViewTitleColour(contentViewBig!!, title_clr)
            setCustomContentViewTitleColour(contentViewSmall!!, title_clr)
            setCustomContentViewExpandedBackgroundColour(contentViewBig!!, pt_bg)
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)
            setCustomContentViewMessageColour(contentViewBig!!, message_clr)
            setCustomContentViewMessageColour(contentViewSmall!!, message_clr)

            val launchIntent = Intent(context, FCM_TARGET_ACTIVITY)
            launchIntent.putExtras(extras)
            launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            launchIntent.action = java.lang.Long.toString(System.currentTimeMillis())
            val pIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
            )
            bitmapImage = getBitmapfromUrl(image)
            if(bitmapImage!=null) {
                contentViewBig!!.setImageViewBitmap(R.id.big_image, bitmapImage)
                contentViewSmall!!.setImageViewBitmap(R.id.large_icon, bitmapImage)
            }


            contentViewSmall!!.setImageViewResource(
                R.id.small_icon,
                FCM_ICON
            )
            contentViewBig!!.setImageViewResource(
                R.id.small_icon,
                FCM_ICON
            )
//
//            setCustomContentViewDotSep(contentViewBig);
//            setCustomContentViewDotSep(contentViewSmall);
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val id = "messenger_general"
            val name = "General"
            val description = "General Notifications sent by the app"
            val rand = Random()
            val a = rand.nextInt(101) + 1
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this, id)
                //.setLargeIcon(image)/*Notification icon image*/
                .setSmallIcon(FCM_ICON)
                .setContentTitle(title)
                .setContentText(message)
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
            Log.d(TAG, "renderOneBezelNotification: ")
        } catch (t: Throwable) {
            Log.d(TAG, "renderOneBezelNotification: $t")
        }
    }


    fun isValidUrl(urlString: String?): Boolean {
        try {
            val url = URL(urlString)
            return URLUtil.isValidUrl(urlString) && Patterns.WEB_URL.matcher(urlString).matches()
        } catch (ignored: MalformedURLException) {
        }
        return false
    }


    /*
     *To get a Bitmap image from the URL received
     * */
    fun getBitmapfromUrl(imageUrl: String?): Bitmap? {
        var bitmap:Bitmap? = null
        if(image == null || image.equals("") || !isValidUrl(imageUrl)){
            return bitmap;
        }
        try {
            val t:Thread = Thread{
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                bitmap = BitmapFactory.decodeStream(input)
            }
            t.start()
            t.join()
            return bitmap
        } catch (e: Exception) {
            Log.d(TAG, "getBitmapfromUrl: $e")
            return bitmap
        }
    }


    fun fetchNotifications(context: Context) {
        try {
            Log.d(TAG, "fetchNotifications: called " + context.packageName)
            retrofit = APIClient.getClient()
            apiInterface = retrofit?.create(APIInterface::class.java)
            getAppName(context)
            notificationListObservable = apiInterface?.getNotifications(context.packageName)
            notificationListObservable?.subscribeOn(Schedulers.newThread())!!.observeOn(
                AndroidSchedulers.mainThread()
            )?.subscribe(
                {
                    setNotificationData(it, context)
                },
                {
                    Log.d(TAG, "fetchNotifications error: " + it.message)
                })
        } catch (e: Exception) {
            Log.d(TAG, "fetchNotifications: catch message " + e.message)
            e.printStackTrace()
        }

    }

    fun setNotificationData(notificationList: ArrayList<NotificationPayloadModel>, context: Context){
        try {
            Log.d(TAG, "setNotificationData: called")
            val sharedPreferences = context.getSharedPreferences(
                "missedNotifications",
                MODE_PRIVATE
            )
            for (notificationObject: NotificationPayloadModel in notificationList) {
                if(sharedPreferences.contains(notificationObject.id)){
                    continue
                }
                val extras = jsonToBundle(JSONObject(notificationObject.data))
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA
                ).apply {
                    // setting the small icon for notification
                    if (metaData.containsKey("FCM_ICON")) {
                        Log.d(TAG, "FCM_ICON: " + metaData.get("FCM_ICON"))
                        FCM_ICON = metaData.getInt("FCM_ICON")
                    }
                    //getting and setting the target activity that is to be opened on notification click
                    if (extras.containsKey("target_activity")) {
                        FCM_TARGET_ACTIVITY = Class.forName(
                            extras.getString(
                                "target_activity",
                                ""
                            )
                        ) as Class<out Activity?>?
                    } else if (FCM_TARGET_ACTIVITY == null) {
                        FCM_TARGET_ACTIVITY = Class.forName(
                            metaData.get("FCM_TARGET_ACTIVITY").toString()
                        ) as Class<out Activity?>?
                    }
                }
                setUp(context, extras)
                when (extras.getString("notificationType", "")) {
                    "R" -> {
                        setUp(context, extras)
                        renderRatingNotification(context, extras)
                    }
                    "Z" -> {
                        setUp(context, extras)
                        renderZeroBezelNotification(context, extras)
                    }
                    "O" -> {
                        setUp(context, extras)
                        renderOneBezelNotification(context, extras)
                    }
                    else -> {
                        Log.d(TAG, "onMessageReceived: in else part")
                        sendNotification(context, extras)
                    }
                }
                sharedPreferences.edit().putString(notificationObject.id, notificationObject.data).apply()
            }
        } catch (e: Exception){
            Log.d(TAG, "setNotificationData: catch " + e.message)
            e.printStackTrace()
        }
    }

    fun jsonToBundle(jsonObject: JSONObject): Bundle {
        val bundle = Bundle()
        val iter: Iterator<*> = jsonObject.keys()
        while (iter.hasNext()) {
            val key = iter.next() as String
            val value = jsonObject.getString(key)
            bundle.putString(key, value)
        }
        return bundle
    }

    fun checkForNotifications(
        context: Context,
        intent: Intent,
        webViewActivityToOpen: Class<out Activity?>?,
        activityToOpen: Class<out Activity?>?,
        intentParam: String
    )
    {
        try {
            if(!intent.hasExtra("rating") && !intent.hasExtra("which")){
                fetchNotifications(context)
            }
            val rating: Int = intent.getIntExtra("rating", 0)
            Log.i("Result", "Got the data " + intent.getIntExtra("rating", 0))
            var showWhich = true
            if (intent.hasExtra("rating")) {
                if (rating == 5) {
                    val url = intent.getStringExtra("link")
                    showWhich = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val manager = ReviewManagerFactory.create(context)
                        val request = manager.requestReviewFlow()
                        request.addOnCompleteListener { task: Task<ReviewInfo?> ->
                            if (task.isSuccessful) {
                                // We can get the ReviewInfo object
                                val reviewInfo = task.result
                                val myActivity: Activity = context as Activity
                                val flow = manager.launchReviewFlow(myActivity, reviewInfo)
                                flow.addOnCompleteListener { taask: Task<Void?>? ->
                                    Log.d("main", "inAppreview: completed")
                                }
                            } else {
                                // There was some problem, continue regardless of the result.
                                Log.d("inAppreview", "checkForNotis: failed")
                            }
                        }
                    }else{
                        val intent1 = Intent(
                            Intent.ACTION_VIEW, Uri.parse(
                                "https://play.google.com/store/apps/details?id=$url"
                            )
                        )
                        context.startActivity(intent1)
                    }
                } else if (rating > 0) {
                    Toast.makeText(context, "Thanks for your feedback :)", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            if (intent.hasExtra("which") && showWhich) {
                val which = intent.getStringExtra("which")
                var url = ""
                if(intent.hasExtra("link")){
                    url = intent.getStringExtra("link")!!
                } else if(intent.hasExtra("url")) {
                    url = intent.getStringExtra("url")!!
                }
                val extras : Bundle? = intent.extras;
                when (which) {
                    "B" -> {
                        try {
                            val intent1 = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent1)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                "Unable to open the link",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                    "P" -> {
                        try {
                            val intent1 = Intent(
                                Intent.ACTION_VIEW, Uri.parse(
                                    "market://details?id=$url"
                                )
                            )
                            context.startActivity(intent1)
                        } catch (e: ActivityNotFoundException) {
                            e.printStackTrace()
                            val intent1 = Intent(
                                Intent.ACTION_VIEW, Uri.parse(
                                    "https://play.google.com/store/apps/details?id=$url"
                                )
                            )
                            context.startActivity(intent1)
                        }
                    }
                    "L" -> {
                        try {
                            val intent1 = Intent(context, webViewActivityToOpen)
                            intent1.putExtra("link", url)
                            if (extras != null) {
                                intent1.putExtras(extras)
                            }
                            context.startActivity(intent1)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    "D" -> {
                        try {
                            val intent1 = Intent(context, activityToOpen)
                            intent1.putExtra(intentParam, url)
                            intent1.putExtra("link", url)
                            if (extras != null) {
                                intent1.putExtras(extras)
                            }
                            context.startActivity(intent1)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    else -> {
                        Log.d(TAG, "No event fired")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkForNotifications: \$e")
//                Dont push
        }
    }


    override fun onInAppButtonClick(hashMap: HashMap<String?, String?>) {
        Log.d("inApp", "onInAppButtonClick: $hashMap")
        val extras = Bundle()
        for ((key, value) in hashMap.entries) {
            extras.putString(key, value)
            Log.d("extras", "-> $extras")
        }
        Log.d("inApp", "onInAppButtonClick: " + extras)
        checkForInAppNotifications(
            inAppContext,
            extras,
            inAppWebViewActivityToOpen,
            inAppActivityToOpen,
            inAppIntentParam
        )
    }

//    override fun messageClicked(inAppMessage: InAppMessage, action: Action) {
//        // Determine which URL the user clicked
//        val url = action.actionUrl
//        val activity1 = inAppContext as Activity
//        (inAppContext as Activity).finish()
//        inAppContext.startActivity(activity1.intent)
//
//        val dataBundle: Map<String, String>? = inAppMessage.data
//        Log.d(TAG, "messageClicked: " + dataBundle.toString())
//        val extras = Bundle()
//        for ((key, value) in dataBundle!!.entries) {
//            extras.putString(key, value)
//        }
//
//        checkForInAppNotifications(
//            inAppContext,
//            extras,
//            inAppWebViewActivityToOpen,
//            inAppActivityToOpen,
//            inAppIntentParam
//        )
//    }


    fun setListener(
        context: Context,
        webViewActivityToOpen: Class<out Activity?>?,
        activityToOpen: Class<out Activity?>?,
        intentParam: String
    ) {
        if(CleverTapAPI.getDefaultInstance(context)!=null){
            CleverTapAPI.getDefaultInstance(context)!!.setInAppNotificationButtonListener(this)
        }
        inAppContext = context
        inAppWebViewActivityToOpen = webViewActivityToOpen
        inAppActivityToOpen = activityToOpen
        inAppIntentParam = intentParam
    }



    fun checkForInAppNotifications(
        context: Context,
        extras: Bundle,
        webViewActivityToOpen: Class<out Activity?>?,
        activityToOpen: Class<out Activity?>?,
        intentParam: String
    )
    {

        Log.d("inApp", "onInAppButtonClick:  inside")
        try {
            if (extras.containsKey("which")) {
                val which = extras.getString("which")
                val url = extras.getString("link")
                val title = extras.getString("title")

                when (which) {
                    "B" -> {
                        try {
                            val intent1 = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent1)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                "Unable to open the link",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                    "P" -> {
                        try {
                            val intent1 = Intent(
                                Intent.ACTION_VIEW, Uri.parse(
                                    "market://details?id=$url"
                                )
                            )
                            context.startActivity(intent1)
                        } catch (e: ActivityNotFoundException) {
                            e.printStackTrace()
                            val intent1 = Intent(
                                Intent.ACTION_VIEW, Uri.parse(
                                    "https://play.google.com/store/apps/details?id=$url"
                                )
                            )
                            context.startActivity(intent1)
                        }
                    }
                    "L" -> {
                        try {
                            val intent1 = Intent(context, webViewActivityToOpen)
                            intent1.putExtras(extras)
                            context.startActivity(intent1)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    "D" -> {
                        try {
                            val intent1 = Intent(context, activityToOpen)
                            intent1.putExtra(intentParam, url)
                            intent1.putExtras(extras)
                            context.startActivity(intent1)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    else -> {
                        Log.d(TAG, "No event fired")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkForInAppNotifications: $e")
//                Dont push
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
        contentView.setViewVisibility(R.id.subtitle, View.GONE)
        contentView.setViewVisibility(R.id.sep_subtitle, View.GONE)
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


    private fun setDotSep(context: Context) {
        try {
            pt_dot = context.resources.getIdentifier(
                "dot_sep",
                "drawable",
                context.packageName
            )
            dot_sep = Utils.setBitMapColour(context, pt_dot, meta_clr)
        } catch (e: NullPointerException) {
//            PTLog.debug("NPE while setting dot sep color");
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

    private fun setUp(context: Context, extras: Bundle?) {
        message = extras!!.getString("message")
        if(message==null || message.equals("")){
            message = extras!!.getString("nm")
        }
        messageBody = extras!!.getString("messageBody")
        message_clr = extras!!.getString("message_clr")
        title = extras!!.getString("title")
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

    companion object {
        private const val TAG = "FirebaseMessageService"
        var bitmapImage: Bitmap? = null
            private set
    }

}