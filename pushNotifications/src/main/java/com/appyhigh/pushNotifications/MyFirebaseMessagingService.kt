package com.appyhigh.pushNotifications

import android.annotation.SuppressLint
import android.app.*
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.text.Html
import android.util.Log
import android.util.Patterns
import android.view.View
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.appyhigh.pushNotifications.apiclient.APIClient
import com.appyhigh.pushNotifications.apiclient.APIInterface
import com.appyhigh.pushNotifications.models.BitmapLoadListener
import com.appyhigh.pushNotifications.models.NotificationPayloadModel
import com.appyhigh.pushNotifications.utils.Constants
import com.appyhigh.pushNotifications.utils.Constants.FCM_COLOR
import com.appyhigh.pushNotifications.utils.Constants.FCM_ICON
import com.appyhigh.pushNotifications.utils.Constants.FCM_TARGET_ACTIVITY
import com.appyhigh.pushNotifications.utils.Constants.FCM_TARGET_SERVICE
import com.appyhigh.pushNotifications.utils.RSAKeyGenerator
import com.appyhigh.pushNotifications.utils.Utils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.NotificationTarget
import com.bumptech.glide.request.target.Target
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.InAppNotificationButtonListener
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*


class MyFirebaseMessagingService() : FirebaseMessagingService(), InAppNotificationButtonListener {

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
    private val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
    } else{
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
    }

    constructor(context: Context, checkForNotificationPermission:Boolean = false, viewForSnackbar:View?=null) : this() {
        if(checkForNotificationPermission) {
            checkNotificationPermissions(context, viewForSnackbar)
        }
    }

    fun checkNotificationPermissions(context: Context, viewForSnackbar: View?){
        try{
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
                if (!areNotificationsEnabled) {
                    // Because the user took an action to create a notification, we create a prompt to let
                    // the user re-enable notifications for this application again.
                    val snackbar: Snackbar = Snackbar.make(viewForSnackbar!!, "You need to enable notifications for this app", Snackbar.LENGTH_LONG)
                        .setAction("ENABLE", View.OnClickListener { // Links to this app's notification settings
                            openNotificationSettingsForApp(context)
                        })
                    snackbar.show()
                    return
                }
            }
        } catch (ex:java.lang.Exception){
            ex.printStackTrace()
        }
    }

    private fun openNotificationSettingsForApp(context: Context) {
        // Links to this app's notification settings.
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("app_package", context.packageName)
        intent.putExtra("app_uid", context.applicationInfo.uid)
        // for Android 8 and above
        intent.putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
        context.startActivity(intent)
    }

    fun addTopics(context: Context, appName: String, isDebug: Boolean) {
        if (appName.equals("")) {
            getAppName(context)
        } else {
            this.appName = appName
        }
        if (isDebug) {
            firebaseSubscribeToTopic(appName + "Debug")
        } else {
            firebaseSubscribeToTopic(appName)
            try{
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                firebaseSubscribeToTopic(appName+info.versionName)
            } catch (ex:Exception){
                ex.printStackTrace()
            }
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
        } else {
            appName = context.getString(stringId)
        }
        appName = appName.replace("\\s".toRegex(), "")
        appName = appName.toLowerCase()
    }

    fun firebaseSubscribeToTopic(appName: String) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(appName)
                .addOnCompleteListener { task ->
                    var msg = "subscribed to $appName"
                    if (!task.isSuccessful) {
                        msg = "not subscribed to $appName"
                    }

                    Log.d(TAG, msg)
                }
        } catch (e: Exception) {
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
            if (remoteMessage.data.isNotEmpty()) {
                Log.d(TAG, "Message data payload: " + remoteMessage.data)

                //added to check if addressNotification is unread
                if(remoteMessage.data["notificationFrom"] == "MY_ADDRESS"){
                    val preference = getSharedPreferences("notificationData", MODE_PRIVATE)
                    preference.edit().putBoolean("isNotification",true).apply()
                }
                // Check if message contains a notification payload.
                if (remoteMessage.notification != null) {
                    Log.d(TAG, "Message Notification Body: " + remoteMessage.notification!!.body)
                }
                val extras = Bundle()
                for ((key, value) in remoteMessage.data) {
                    extras.putString(key, value)
                }
                if (remoteMessage.data.containsKey("notification_source")
                    && remoteMessage.data["notification_source"].equals("flyy_sdk", ignoreCase = true)) {
                    onMessageReceivedListener?.onMessageReceived(remoteMessage)
                } else{
                    onMessageReceivedListener?.onMessageReceived(extras)
                    //Added time stamp for notifications
                    val format: Long = System.currentTimeMillis()
                    extras.putString("timestamp",format.toString())
                    val notificationType = extras.getString("notificationType")
                    val sharedPreferences = getSharedPreferences("missedNotifications", MODE_PRIVATE)
                    sharedPreferences.edit().putString(
                        extras.getString("link", "default"),
                        extras.toString()
                    ).apply()
                    packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).apply {
                        // setting the small icon for notification
                        if (metaData.containsKey("FCM_ICON")) {
                            Log.d(TAG, "onMessageReceived: " + metaData.get("FCM_ICON"))
                            FCM_ICON = metaData.getInt("FCM_ICON")
                        }
                        try{
                            if(metaData.containsKey("FCM_COLOR")){
                                FCM_COLOR = metaData.getInt("FCM_COLOR")
                            }
                        } catch (ex:Exception){}
                        //getting and setting the target activity that is to be opened on notification click
                        if (extras.containsKey("target_activity")) {
                            FCM_TARGET_ACTIVITY = Class.forName(
                                extras.getString(
                                    "target_activity",
                                    ""
                                )
                            ) as Class<out Activity?>?
                        } else if (FCM_TARGET_ACTIVITY == null) {
                            Log.d(TAG, "onMessageReceived: " + metaData.get("FCM_TARGET_ACTIVITY"))
                            FCM_TARGET_ACTIVITY = Class.forName(
                                metaData.get("FCM_TARGET_ACTIVITY").toString()
                            ) as Class<out Activity?>?
                        }
                        try {
                            //getting and setting the target service that that needs to be opened
                            if (extras.containsKey("target_service")) {
                                FCM_TARGET_SERVICE = Class.forName(
                                    extras.getString(
                                        "target_service",
                                        ""
                                    )
                                ) as Class<out IntentService?>?
                            } else if (FCM_TARGET_SERVICE == null) {
                                Log.d(TAG, "onMessageReceived: " + metaData.get("FCM_TARGET_SERVICE"))
                                FCM_TARGET_SERVICE = Class.forName(
                                    metaData.get("FCM_TARGET_SERVICE").toString()
                                ) as Class<out IntentService?>?
                            }
                        } catch(ex: Exception){
                            ex.printStackTrace()
                        }
                    }
                    val info = CleverTapAPI.getNotificationInfo(extras)
                    if (info.fromCleverTap) {
                        if (extras.getString("nm") != null && extras.getString("nm") != "") {
                            val message = extras.getString("message") ?: extras.getString("nm")
                            if (message != null) {
                                if (message != "") {
                                    image = extras.getString("image")
                                    getBitmapFromUrl(image, object : BitmapLoadListener{
                                        override fun onSuccess(bitmap: Bitmap?) {
                                            try{
                                                bitmapImage = bitmap
                                                when (notificationType) {
                                                    "N" -> {
                                                        sendNativeNotification(this@MyFirebaseMessagingService, extras)
                                                    }
                                                    "R" -> {
                                                        renderRatingNotification(this@MyFirebaseMessagingService, extras)
                                                    }
                                                    "Z" -> {
                                                        renderZeroBezelNotification(this@MyFirebaseMessagingService, extras)
                                                    }
                                                    "O" -> {
                                                        renderOneBezelNotification(this@MyFirebaseMessagingService, extras)
                                                    }
                                                    "A" -> {
                                                        startService(this@MyFirebaseMessagingService, extras)
                                                    }
                                                    "imageWithHeading" -> {
                                                        imageWithHeading(this@MyFirebaseMessagingService,extras)
                                                    }
                                                    "imageWithSubHeading" -> {
                                                        imageWithSubHeading(this@MyFirebaseMessagingService, extras)
                                                    }
                                                    "smallTextImageCard" -> {
                                                        setSmallTextImageCard(this@MyFirebaseMessagingService,extras)
                                                    }
                                                    else -> {
                                                        sendNotification(this@MyFirebaseMessagingService, extras)
                                                    }
                                                }
                                            } catch (ex:Exception){
                                                ex.printStackTrace()
                                            }
                                        }
                                    })
                                } else {
                                    CleverTapAPI.getDefaultInstance(this)!!.pushNotificationViewedEvent(extras)
                                }
                            }
                        }
                    } else {
                        setUp(this, extras)
                        image = extras.getString("image")
                        getBitmapFromUrl(image, object : BitmapLoadListener{
                            override fun onSuccess(bitmap: Bitmap?) {
                                try{
                                    bitmapImage = bitmap
                                    when (notificationType) {
                                        "N" -> {
                                            Log.d(TAG, "onMessageReceived: in native part")
                                            sendNativeNotification(this@MyFirebaseMessagingService, extras)
                                        }
                                        "R" -> {
                                            renderRatingNotification(this@MyFirebaseMessagingService, extras)
                                        }
                                        "Z" -> {
                                            renderZeroBezelNotification(this@MyFirebaseMessagingService, extras)
                                        }
                                        "O" -> {
                                            renderOneBezelNotification(this@MyFirebaseMessagingService, extras)
                                        }
                                        "A" -> {
                                            startService(this@MyFirebaseMessagingService, extras)
                                        }
                                        "imageWithHeading" -> {
                                            imageWithHeading(this@MyFirebaseMessagingService, extras)
                                        }
                                        "imageWithSubHeading" -> {
                                            imageWithSubHeading(this@MyFirebaseMessagingService, extras)
                                        }
                                        "smallTextImageCard" -> {
                                            setSmallTextImageCard(this@MyFirebaseMessagingService, extras)
                                        }
                                        else -> {
                                            Log.d(TAG, "onMessageReceived: in else part")
                                            sendNotification(this@MyFirebaseMessagingService, extras)
                                        }
                                    }
                                } catch (ex:Exception){
                                    ex.printStackTrace()
                                }
                            }
                        })
                    }
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startService(context: Context, extras: Bundle) {
        try {
            if (FCM_TARGET_SERVICE != null) {
                val intent = Intent(context.applicationContext, FCM_TARGET_SERVICE)
                intent.putExtra("bundleData", extras)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendNotification(context: Context, extras: Bundle) {
        try {
            var message = extras.getString("message")
                ?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
            val url = extras.getString("link")
            var which = extras.getString("which")
            var title = extras.getString("title")
                ?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
            if (message == null || message.equals("")) {
                message = extras.getString("nm")
                    ?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
            }
            if (title == null || title.equals("")) {
                title = extras.getString("nt")
                    ?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
            }
            Log.i("Result", "Got the data yessss")
            //added to put extras to intent when notification is from address module
            val notificationFrom = extras.getString("notificationFrom")
            if(notificationFrom != null && notificationFrom == "MY_ADDRESS"){
                extras.putString("addressNotification","true")
            }
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
                flags
            )
            val preference = context.getSharedPreferences(Constants.PUSH_LIB_PREFS, MODE_PRIVATE)
            val isGrouping = preference.getBoolean(Constants.ENABLE_NOTIFICATION_GROUPING, true)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (title != null && message != null) {
                val notificationBuilder: NotificationCompat.Builder
                if (bitmapImage == null) {
                    notificationBuilder = NotificationCompat.Builder(context.applicationContext)
                        .setSmallIcon(FCM_ICON)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(Notification.PRIORITY_DEFAULT)

                    //added a check for address notification
                    if(notificationFrom != "MY_ADDRESS"){
                        try{
                            val bitmap = Picasso.get().load(extras.getString("image")).get()
                            notificationBuilder.setLargeIcon(bitmap)
                            notificationBuilder.setStyle(NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap))
                        } catch (ex:java.lang.Exception){
                            ex.printStackTrace()
                        }
                    }
                } else {
                    notificationBuilder = NotificationCompat.Builder(context.applicationContext)
                        .setLargeIcon(bitmapImage) /*Notification icon image*/
                        .setSmallIcon(FCM_ICON)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmapImage)
                        ) /*Notification with Image*/
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(Notification.PRIORITY_DEFAULT)
                }
                if(FCM_COLOR != 0){
                    notificationBuilder.color = FCM_COLOR
                }
                if(isGrouping){
                    notificationBuilder.setGroup("pushLib"+a+1).setGroupSummary(true)
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
                CleverTapAPI.getDefaultInstance(context)?.pushNotificationViewedEvent(extras)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendNativeNotification(context: Context, extras: Bundle){
        try{
            val rand = Random()
            val a = rand.nextInt(101) + 1
            contentViewBig = RemoteViews(context.packageName, R.layout.cv_big_native)
            contentViewSmall = RemoteViews(context.packageName, R.layout.cv_small_native)
            contentViewSmall!!.setTextViewText(R.id.app_name, Utils.getApplicationName(context))
            contentViewBig!!.setTextViewText(R.id.app_name, Utils.getApplicationName(context))
            contentViewSmall!!.setImageViewResource(R.id.small_icon, FCM_ICON)
            contentViewBig!!.setImageViewResource(R.id.small_icon, FCM_ICON)
            setCustomContentViewTitle(contentViewBig!!, title)
            setCustomContentViewTitle(contentViewSmall!!, title)
            setCustomContentViewTitleColour(contentViewBig!!, title_clr)
            setCustomContentViewTitleColour(contentViewSmall!!, title_clr)
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R){
                contentViewSmall!!.setViewVisibility(R.id.llApp, View.GONE)
            } else{
                contentViewSmall!!.setViewVisibility(R.id.llApp, View.VISIBLE)
            }
            val notificationFrom = extras.getString("notificationFrom")
            if(notificationFrom != null && notificationFrom == "MY_ADDRESS"){
                extras.putString("addressNotification","true")
            }
            val launchIntent = Intent(context, FCM_TARGET_ACTIVITY)
            launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            launchIntent.action = System.currentTimeMillis().toString()
            launchIntent.putExtras(extras)
            val pIntent = PendingIntent.getActivity(context, 0, launchIntent, flags)
            val shareIntent = Intent(context, PushTemplateReceiver::class.java)
            shareIntent.action = "SHARE_CLICK"
            shareIntent.putExtra("onSharePostClicked", true)
            shareIntent.putExtra("notificationId", a + 1)
            shareIntent.putExtras(extras)
            val shareFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else{
                0
            }
            val sharePendingIntent = PendingIntent.getBroadcast(context, Random().nextInt(), shareIntent,
                shareFlags)
            contentViewBig!!.setOnClickPendingIntent(R.id.shareIconLayout, sharePendingIntent)
            if(isDarkTheme()){
                contentViewBig!!.setInt(R.id.share_icon, "setColorFilter", Color.WHITE)
            }
            if(bitmapImage == null){
                contentViewSmall!!.setViewVisibility(R.id.big_image, View.GONE)
                contentViewBig!!.setViewVisibility(R.id.imageLayout, View.GONE)
            }
            var useGlide = false
            //added a check for address notification
            if(notificationFrom != "MY_ADDRESS") {
                try{
                    if (bitmapImage != null) {
                        contentViewBig!!.setImageViewBitmap(R.id.big_image, bitmapImage)
                        contentViewSmall!!.setImageViewBitmap(R.id.big_image, bitmapImage)
                        contentViewBig!!.setViewVisibility(R.id.share_icon, View.GONE)
                        contentViewBig!!.setViewVisibility(R.id.share_icon_light, View.VISIBLE)
                        contentViewBig!!.setTextColor(R.id.title, Color.WHITE)
                    } else{
                        useGlide = true
                        contentViewSmall!!.setViewVisibility(R.id.big_image, View.GONE)
                        contentViewBig!!.setViewVisibility(R.id.imageLayout, View.GONE)
                    }
                } catch (ex:java.lang.Exception){
                    ex.printStackTrace()
                }
            }
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val id = "messenger_general"
            val name = "General"
            val description = "General Notifications sent by the app"
            val preference = context.getSharedPreferences(Constants.PUSH_LIB_PREFS, MODE_PRIVATE)
            val isGrouping = preference.getBoolean(Constants.ENABLE_NOTIFICATION_GROUPING, true)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this, id)
                .setSmallIcon(FCM_ICON)
                .setContentTitle(title)
                .setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewBig)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pIntent)
                .setPriority(Notification.PRIORITY_DEFAULT)
            if(FCM_COLOR != 0){
                notificationBuilder.color = FCM_COLOR
            }

            if(useGlide){
                try{
                    val glideImage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        GlideUrl(image, LazyHeaders.Builder().addHeader("User-Agent", WebSettings.getDefaultUserAgent(context)).build())
                    } else {
                        GlideUrl(image, LazyHeaders.Builder().addHeader("User-Agent", "Mozilla/5.0(Linux;Android12;RMX2161Build/SP1A.210812.016;wv)AppleWebKit/537.36(KHTML,likeGecko)Version/4.0Chrome/104.0.5112.97MobileSafari/537.36").build())
                    }
                    val expandedNotificationTarget = NotificationTarget(context, R.id.big_image, contentViewBig, notificationBuilder.build(),(a+1))
                    Glide.with(this)
                        .asBitmap()
                        .load(glideImage).listener(object: RequestListener<Bitmap>{
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                               return true
                            }

                            override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                if(resource!=null){
                                    contentViewBig!!.setViewVisibility(R.id.share_icon, View.GONE)
                                    contentViewBig!!.setViewVisibility(R.id.share_icon_light, View.VISIBLE)
                                    contentViewBig!!.setTextColor(R.id.title, Color.WHITE)
                                    contentViewBig!!.setImageViewBitmap(R.id.big_image, resource)
                                    contentViewSmall!!.setImageViewBitmap(R.id.big_image, resource)
                                    contentViewSmall!!.setViewVisibility(R.id.big_image, View.VISIBLE)
                                    contentViewBig!!.setViewVisibility(R.id.imageLayout, View.VISIBLE)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        notificationManager.notify(a + 1, notificationBuilder.setChannelId(id).build())
                                    } else {
                                        notificationManager.notify(a + 1, notificationBuilder.build())
                                    }
                                }
                                return true
                            }
                        }).into(expandedNotificationTarget)
                } catch (ex:java.lang.Exception){
                    ex.printStackTrace()
                }
            }
            if(isGrouping){
                notificationBuilder.setGroup("pushLib"+a+1).setGroupSummary(true)
            }
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
            } else {
                notificationManager?.notify(a + 1, notificationBuilder.build())
            }
            Log.d(TAG, "sendNativeNotification: ")
            CleverTapAPI.getDefaultInstance(context)?.pushNotificationViewedEvent(extras)
        } catch (ex:java.lang.Exception){
            ex.printStackTrace()
        }
    }

    private fun renderRatingNotification(context: Context, extras: Bundle) {
        try {
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

            //Set the rating stars
            contentViewRating!!.setImageViewResource(R.id.star1, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star2, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star3, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star4, R.drawable.pt_star_outline)
            contentViewRating!!.setImageViewResource(R.id.star5, R.drawable.pt_star_outline)


//            setCustomContentViewBigImage(contentViewRating, image);
            if (bitmapImage != null) {
                contentViewRating!!.setImageViewBitmap(R.id.big_image, bitmapImage)
                //            setCustomContentViewLargeIcon(contentViewSmall, large_icon);
                contentViewSmall!!.setImageViewBitmap(R.id.large_icon, bitmapImage)
            }


            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val id = "messenger_general"
            val name = "General"
            val description = "General Notifications sent by the app"
            val rand = Random()
            val a = rand.nextInt(101) + 1

            val notificationIntent1 = Intent(context, PushTemplateReceiver::class.java)
            notificationIntent1.putExtra("clicked", 1)
            notificationIntent1.putExtra("notificationId", a + 1)
            notificationIntent1.putExtras(extras)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else{
                0
            }
            val contentIntent1 = PendingIntent.getBroadcast(
                context,
                Random().nextInt(),
                notificationIntent1,
                flags
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
                flags
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
                flags
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
                flags
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
                flags
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
            if(FCM_COLOR != 0){
                notificationBuilder.color = FCM_COLOR
            }

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
            } else {
                notificationManager?.notify(a + 1, notificationBuilder.build())
            }
            CleverTapAPI.getDefaultInstance(context)?.pushNotificationViewedEvent(extras)
            Log.d(TAG, "renderRatingNotification: ")

//            Utils.raiseNotificationViewed(context, extras, config);
        } catch (t: Throwable) {
            Log.d(TAG, "renderRatingNotification: $t")
        }
    }

    private fun renderZeroBezelNotification(context: Context, extras: Bundle) {
        try {
            contentViewBig = RemoteViews(context.packageName, R.layout.zero_bezel)
            setCustomContentViewBasicKeys(contentViewBig!!, context)
            contentViewSmall = RemoteViews(context.packageName, R.layout.cv_small_zero_bezel)

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R){
                contentViewSmall!!.setViewVisibility(R.id.metadata, View.GONE)
                contentViewBig!!.setViewVisibility(R.id.metadata, View.GONE)
            }
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
                flags
            )
            if (bitmapImage != null) {
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
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
            if(FCM_COLOR != 0){
                notificationBuilder.color = FCM_COLOR
            }
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
            } else {
                notificationManager?.notify(a + 1, notificationBuilder.build())
            }
            CleverTapAPI.getDefaultInstance(context)?.pushNotificationViewedEvent(extras)
            Log.d(TAG, "renderZeroBezelNotification: ")
        } catch (t: Throwable) {
            Log.d(TAG, "renderZeroBezelNotification: $t")
        }
    }

    private fun imageWithHeading(context: Context, extras: Bundle) {
        try {
            contentViewBig = RemoteViews(context.packageName, R.layout.image_wt_heading)
            contentViewBig!!.setTextViewText(R.id.app_name, Utils.getApplicationName(context))
            if (meta_clr != null && !meta_clr!!.isEmpty()) {
                contentViewBig!!.setTextColor(
                    R.id.app_name,
                    Utils.getColour(meta_clr, "#FFFFFF")
                )
            }
            contentViewSmall = RemoteViews(context.packageName, R.layout.image_wt_heading_small)
            contentViewSmall!!.setTextViewText(R.id.app_name, Utils.getApplicationName(context))
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R){
                contentViewSmall!!.setViewVisibility(R.id.small_icon, View.GONE)
                contentViewSmall!!.setViewVisibility(R.id.app_name, View.GONE)
                contentViewBig!!.setViewVisibility(R.id.rlApp, View.GONE)
            }

            if (meta_clr != null && !meta_clr!!.isEmpty()) {
                contentViewSmall!!.setTextColor(
                    R.id.app_name,
                    Utils.getColour(meta_clr, "#000000")
                )
            }

//            setCustomAppContentSmall(contentViewSmall!!, context)

            setCustomContentViewTitle(contentViewBig!!, title)
            setCustomContentViewTitle(contentViewSmall!!, title)
//            setCustomContentViewMessage(contentViewBig!!, message)
            setCustomContentViewMessage(contentViewSmall!!, message)

//            setCustomContentViewMessageSummary(contentViewBig!!, messageBody)
            setCustomContentViewTitleColour(contentViewBig!!, title_clr)
            setCustomContentViewTitleColour(contentViewSmall!!, title_clr)
            setCustomContentViewExpandedBackgroundColour(contentViewBig!!, pt_bg)
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)
//            setCustomContentViewMessageColour(contentViewBig!!, message_clr)
            setCustomContentViewMessageColour(contentViewSmall!!, message_clr)

            val launchIntent = Intent(context, FCM_TARGET_ACTIVITY)
            launchIntent.putExtras(extras)
            launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            launchIntent.action = java.lang.Long.toString(System.currentTimeMillis())
            val pIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                flags
            )
            if (bitmapImage != null) {
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
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
                //.setStyle( NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewBig)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pIntent)
                .setPriority(Notification.PRIORITY_DEFAULT)

            if(FCM_COLOR != 0){
                notificationBuilder.color = FCM_COLOR
            }
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
            } else {
                notificationManager?.notify(a + 1, notificationBuilder.build())
            }
            CleverTapAPI.getDefaultInstance(context)?.pushNotificationViewedEvent(extras)
            Log.d(TAG, "imageWithHeading: ")
        } catch (t: Throwable) {
            Log.d(TAG, "imageWithHeading: $t")
        }
    }

    private fun imageWithSubHeading( context: Context, extras: Bundle){
        try {
            contentViewBig = RemoteViews(context.packageName, R.layout.image_wt_sub_heading)
//            setCustomContentViewBasicKeys(contentViewRating!!, context)

            contentViewSmall = RemoteViews(context.packageName, R.layout.image_wt_heading_small)
//            setCustomAppContentSmall(contentViewSmall!!, context)
            contentViewSmall!!.setTextViewText(R.id.app_name, Utils.getApplicationName(context))

            if (meta_clr != null && !meta_clr!!.isEmpty()) {
                contentViewSmall!!.setTextColor(
                    R.id.app_name,
                    Utils.getColour(meta_clr, "#000000")
                )
            }

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R){
                contentViewSmall!!.setViewVisibility(R.id.small_icon, View.GONE)
                contentViewSmall!!.setViewVisibility(R.id.app_name, View.GONE)
                contentViewBig!!.setViewVisibility(R.id.heading_layout, View.GONE)
            }
            setCustomContentViewTitle(contentViewBig!!, title)
            setCustomContentViewTitle(contentViewSmall!!, title)
            setCustomContentViewMessage(contentViewBig!!, message)
            setCustomContentViewMessage(contentViewSmall!!, message)
            setCustomContentViewMessageSummary(contentViewBig!!, messageBody)
            setCustomContentViewTitleColour(contentViewBig!!, title_clr)
            setCustomContentViewTitleColour(contentViewSmall!!, title_clr)
            setCustomContentViewMessageColour(contentViewBig!!, message_clr)
            setCustomContentViewMessageColour(contentViewSmall!!, message_clr)
            setCustomContentViewExpandedBackgroundColour(contentViewBig!!, pt_bg)
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)

            val launchIntent = Intent(context, FCM_TARGET_ACTIVITY)
            launchIntent.putExtras(extras)
            launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            launchIntent.action = java.lang.Long.toString(System.currentTimeMillis())

            val pIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                flags
            )
//            setCustomContentViewBigImage(contentViewRating, image);
            if (bitmapImage != null) {
                contentViewBig!!.setImageViewBitmap(R.id.big_image, bitmapImage)
                //            setCustomContentViewLargeIcon(contentViewSmall, large_icon);
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


            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val id = "messenger_general"
            val name = "General"
            val description = "General Notifications sent by the app"
            val rand = Random()
            val a = rand.nextInt(101) + 1

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notificationBuilder = NotificationCompat.Builder(this, id)
//                .setLargeIcon(image)/*Notification icon image*/
                .setSmallIcon(FCM_ICON)
                .setContentTitle(title)
                .setContentText(message)
//                .setStyle(NotificationCompat.BigPictureStyle())
//                .bigPicture(image))/*Notification with Image*/
//                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewBig)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pIntent)
                .setPriority(Notification.PRIORITY_DEFAULT)
            if(FCM_COLOR != 0){
                notificationBuilder.color = FCM_COLOR
            }

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
            } else {
                notificationManager?.notify(a + 1, notificationBuilder.build())
            }
            CleverTapAPI.getDefaultInstance(context)?.pushNotificationViewedEvent(extras)
            Log.d(TAG, "imageWithSubHeading: ")
//            Utils.raiseNotificationViewed(context, extras, config);
        } catch (t: Throwable) {
            Log.d(TAG, "imageWithSubHeading: $t")
        }
    }

    private fun setSmallTextImageCard(context: Context, extras: Bundle){
        try {

            contentViewSmall = RemoteViews(context.packageName, R.layout.image_wt_heading_small)
//            setCustomAppContentSmall(contentViewSmall!!, context)
            contentViewSmall!!.setTextViewText(R.id.app_name, Utils.getApplicationName(context))

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R){
                contentViewSmall!!.setViewVisibility(R.id.small_icon, View.GONE)
                contentViewSmall!!.setViewVisibility(R.id.app_name, View.GONE)
            }
            if (meta_clr != null && !meta_clr!!.isEmpty()) {
                contentViewSmall!!.setTextColor(
                    R.id.app_name,
                    Utils.getColour(meta_clr, "#000000")
                )
            }

            setCustomContentViewTitle(contentViewSmall!!, title)
            setCustomContentViewMessage(contentViewSmall!!, message)
            setCustomContentViewTitleColour(contentViewSmall!!, title_clr)
            setCustomContentViewMessageColour(contentViewSmall!!, message_clr)
            setCustomContentViewCollapsedBackgroundColour(contentViewSmall!!, pt_bg)

            val launchIntent = Intent(context, FCM_TARGET_ACTIVITY)
            launchIntent.putExtras(extras)
            launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            launchIntent.action = java.lang.Long.toString(System.currentTimeMillis())
            val pIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                flags
            )
//            setCustomContentViewBigImage(contentViewRating, image);
            if (bitmapImage != null) {
                //            setCustomContentViewLargeIcon(contentViewSmall, large_icon);
                contentViewSmall!!.setImageViewBitmap(R.id.large_icon, bitmapImage)
            }

            contentViewSmall!!.setImageViewResource(
                R.id.small_icon,
                FCM_ICON
            )


            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val id = "messenger_general"
            val name = "General"
            val description = "General Notifications sent by the app"
            val rand = Random()
            val a = rand.nextInt(101) + 1

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notificationBuilder = NotificationCompat.Builder(this, id)
//                .setLargeIcon(image)/*Notification icon image*/
                .setSmallIcon(FCM_ICON)
                .setContentTitle(title)
                .setContentText(message)
//                .setStyle(NotificationCompat.BigPictureStyle())
//                .bigPicture(image))/*Notification with Image*/
//                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(contentViewSmall)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pIntent)
                .setPriority(Notification.PRIORITY_DEFAULT)
            if(FCM_COLOR != 0){
                notificationBuilder.color = FCM_COLOR
            }

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
            } else {
                notificationManager?.notify(a + 1, notificationBuilder.build())
            }
            CleverTapAPI.getDefaultInstance(context)?.pushNotificationViewedEvent(extras)
            Log.d(TAG, "setSmallTextImageCard: ")
//            Utils.raiseNotificationViewed(context, extras, config);
        } catch (t: Throwable) {
            Log.d(TAG, "setSmallTextImageCard: $t")
        }
    }


    private fun renderOneBezelNotification(context: Context, extras: Bundle) {
        try {
            contentViewBig = RemoteViews(context.packageName, R.layout.one_bezel)
            setCustomContentViewBasicKeys(contentViewBig!!, context)
            contentViewSmall = RemoteViews(context.packageName, R.layout.cv_small_one_bezel)
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R){
                contentViewSmall!!.setViewVisibility(R.id.rlApp, View.GONE)
            }
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
                flags
            )
            if (bitmapImage != null) {
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
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
            if(FCM_COLOR != 0){
                notificationBuilder.color = FCM_COLOR
            }

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
            } else {
                notificationManager?.notify(a + 1, notificationBuilder.build())
            }
            CleverTapAPI.getDefaultInstance(context)?.pushNotificationViewedEvent(extras)
            Log.d(TAG, "renderOneBezelNotification: ")
        } catch (t: Throwable) {
            Log.d(TAG, "renderOneBezelNotification: $t")
        }
    }



    fun isDarkTheme(): Boolean {
        return this.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun isValidUrl(urlString: String?): Boolean {
        try {
            return !urlString.isNullOrEmpty() && URLUtil.isValidUrl(urlString)
                    && Patterns.WEB_URL.matcher(urlString).matches()
        } catch (ignored: MalformedURLException) {
        }
        return false
    }


    /*
     *To get a Bitmap image from the URL received
     * */
    private fun getBitmapFromUrl(imageUrl: String?, listener: BitmapLoadListener) {
        if (!isValidUrl(imageUrl)) {
            listener.onSuccess(null)
        } else{
            CoroutineScope(Dispatchers.Default).launch {
                var bitmap: Bitmap? = null
                try {
                    val url = URL(imageUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input = connection.inputStream
                    bitmap = BitmapFactory.decodeStream(input)
                } catch (e: Exception) {
                    Log.d(TAG, "getBitmapFromUrl: $e")
                }
                launch(Dispatchers.Default){
                    withContext(Dispatchers.Main){
                        listener.onSuccess(bitmap)
                    }
                }
            }
        }
    }



    fun fetchNotifications(context: Context) {
        try {
            Log.d(TAG, "fetchNotifications: called " + context.packageName)
            retrofit = APIClient.getClient()
            apiInterface = retrofit?.create(APIInterface::class.java)
            getAppName(context)
            apiInterface!!.getNotifications(RSAKeyGenerator.getJwtToken()?:"",context.packageName).enqueue(object :
                Callback<ArrayList<NotificationPayloadModel>> {
                override fun onResponse(
                    call: Call<ArrayList<NotificationPayloadModel>>,
                    response: Response<ArrayList<NotificationPayloadModel>>
                ) {
                    try {
                        if (response.body() != null) {
                            setNotificationData(response.body()!!, context)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d(TAG, "fetchNotifications error: " + e.message)
                    }
                }

                override fun onFailure(
                    call: Call<ArrayList<NotificationPayloadModel>>,
                    t: Throwable
                ) {
                    Log.d(TAG, "fetchNotifications error: " + t.message)
                }

            })
        } catch (e: Exception) {
            Log.d(TAG, "fetchNotifications: catch message " + e.message)
            e.printStackTrace()
        }

    }

    fun setNotificationData(
        notificationList: ArrayList<NotificationPayloadModel>,
        context: Context
    ) {
        try {
            Thread {
                Log.d(TAG, "setNotificationData: called")
                val sharedPreferences = context.getSharedPreferences(
                    "missedNotifications",
                    MODE_PRIVATE
                )
                for (notificationObject: NotificationPayloadModel in notificationList) {
                    if (sharedPreferences.contains(notificationObject.id)) {
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
                        try{
                            if(metaData.containsKey("FCM_COLOR")){
                                FCM_COLOR = metaData.getInt("FCM_COLOR")
                            }
                        } catch (ex:Exception){}
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
                    Handler(Looper.getMainLooper()).postDelayed({
                        image = extras.getString("image")
                        getBitmapFromUrl(image, object : BitmapLoadListener{
                            override fun onSuccess(bitmap: Bitmap?) {
                                try{
                                    bitmapImage = bitmap
                                    when (extras.getString("notificationType", "")) {
                                        "N" -> {
                                            Log.d(TAG, "onMessageReceived: in native part")
                                            sendNativeNotification(context, extras)
                                        }
                                        "R" -> {
                                            renderRatingNotification(context, extras)
                                        }
                                        "Z" -> {
                                            renderZeroBezelNotification(context, extras)
                                        }
                                        "O" -> {
                                            renderOneBezelNotification(context, extras)
                                        }
                                        "A" -> {
                                            startService(context, extras)
                                        }
                                        "imageWithHeading" -> {
                                            imageWithHeading(context, extras)
                                        }
                                        "imageWithSubHeading" -> {
                                            imageWithSubHeading(context, extras)
                                        }
                                        "smallTextImageCard" -> {
                                            setSmallTextImageCard(context, extras)
                                        }
                                        else -> {
                                            Log.d(TAG, "onMessageReceived: in else part")
                                            sendNotification(context, extras)
                                        }
                                    }
                                } catch (ex:Exception){
                                    ex.printStackTrace()
                                }
                            }
                        })
                    }, 1000)
                    //added for time stamp
                    val format: Long = System.currentTimeMillis()
                    val parser = JsonParser()
                    val tempObject:JsonObject = parser.parse(notificationObject.data) as JsonObject
                    tempObject.addProperty("timestamp",format)
                    sharedPreferences.edit().putString(
                        notificationObject.id,
                        tempObject.toString()
                    ).apply()
                }
            }.start()
        } catch (e: Exception) {
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
    ) {
        try {
            if (!intent.hasExtra("rating") && !intent.hasExtra("which")) {
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
                    } else {
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
                if (intent.hasExtra("link")) {
                    url = intent.getStringExtra("link")!!
                } else if (intent.hasExtra("url")) {
                    url = intent.getStringExtra("url")!!
                }
                val extras: Bundle? = intent.extras;
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


    fun setListener(
        context: Context,
        webViewActivityToOpen: Class<out Activity?>?,
        activityToOpen: Class<out Activity?>?,
        intentParam: String
    ) {
        if (CleverTapAPI.getDefaultInstance(context) != null) {
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
    ) {

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


    private fun setPendingIntent(
        context: Context,
        extras: Bundle,
        launchIntent: Intent
    ): PendingIntent {
        launchIntent.putExtras(extras)
        launchIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else{
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            context, System.currentTimeMillis().toInt(),
            launchIntent, flags
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

    private fun setCustomAppContentSmall(contentView: RemoteViews, context: Context){
        contentView.setTextViewText(R.id.app_name, Utils.getApplicationName(context))

        if (meta_clr != null && !meta_clr!!.isEmpty()) {
            contentView.setTextColor(
                R.id.app_name,
                Utils.getColour(meta_clr, "#A6A6A6")
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
            ?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
        if (message == null || message.equals("")) {
            message = extras.getString("nm")
                ?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
        }
        messageBody = extras.getString("messageBody")
            ?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
        message_clr = extras.getString("message_clr")
        title = extras.getString("title")
            ?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
        if (title == null || title.equals("")) {
            title = extras.getString("nt")
                ?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
        }
        title_clr = extras.getString("title_clr")
        meta_clr = extras.getString("meta_clr")
        pt_bg = extras.getString("pt_bg")
        image = extras.getString("image")
        large_icon = extras.getString("large_icon")
        small_icon_clr = extras.getString("small_icon_clr")
    }

    companion object {
        private const val TAG = "AppyHighFCMService"
        var onMessageReceivedListener: OnMessageReceivedListener?=null
        var bitmapImage: Bitmap? = null
            private set
    }
}