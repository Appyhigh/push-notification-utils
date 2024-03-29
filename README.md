
# Push Notifications & InApp Notifications
- [Push Notifications](#push-notifications)
- [InApp Notifications](#inApp-notifications)

# Push Notifications

# Table of contents

- [Installation](#installation)
- [Template Types](#template-types)
- [Functions](#functions)

# Installation

[(Back to top)](#table-of-contents)

1.To import this library, Add the following line to your project's *build.gradle* at the end of repositories.
```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}

```
2.To import this library, Add the following line to your app level *build.gradle* file.
```groovy
implementation 'com.clevertap.android:clevertap-android-sdk:4.6.0'(#Recommended latest version)
implementation 'com.google.firebase:firebase-messaging:23.0.7'(#Recommended latest version)
implementation 'com.github.Appyhigh:push-notification-utils:1.3.5'
```
**Note:** Even though you are not using cleverTap, you must include the cleverTap library

3.Add the following line to your *AndroidManifest.xml* for internet permission.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

```
4.If you are using **CleverTap** for push notifications, then add the following lines to your *AndroidManifest.xml* file.

```xml
 <meta-data
    android:name="CLEVERTAP_ACCOUNT_ID"
    android:value="**your_accountId**"/>
<meta-data
    android:name="CLEVERTAP_TOKEN"
    android:value="**your_token**"/>
```

4.To recieve the Push Notifications, Add the following lines to your *AndroidManifest.xml* file.

```xml
<service
    android:name="com.appyhigh.pushNotifications.MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
<receiver
    android:name="com.appyhigh.pushNotifications.PushTemplateReceiver"
    android:exported="false"
    android:enabled="true">
</receiver>
```

5.Add the following lines inside *application* tag to your *AndroidManifest.xml*.

```xml
<meta-data android:name="FCM_TARGET_SERVICE" android:value="**your_target_service with package name**" />
<meta-data android:name="FCM_TARGET_ACTIVITY" android:value="**your_target_activity with package name**" />
<meta-data android:name="FCM_ICON" android:resource="**your_app_icon**" />

<!--Add this only if needed-->
<meta-data android:name="FCM_COLOR" android:resource="**color_reference_from_colors.xml**" /> 
```

FCM_TARGET_ACTIVITY - default activity that should be opened when notification is clicked.

FCM_ICON - notification icon that needs be displayed in the push notification.

FCM_COLOR - background color that needs to be used in notification.

### Example
```xml
<meta-data android:name="FCM_TARGET_ACTIVITY" android:value="messenger.chat.social.messenger.activities.MatchingActivity" />
<meta-data android:name="FCM_ICON" android:resource="@drawable/launcher" />
<meta-data android:name="FCM_COLOR" android:resource="@color/temp_color" />
```


# Template Types

[(Back to top)](#table-of-contents)

We have 4 types of push notifications P, B, L and D which are identified by the which parameter.
- P type notifications must open the play store. ("link": "com.appyhigh")
- B type notifications must open the default browser. ("link": "https://youtube.com")
- L type notifications must open the webview within the app. ("link": "https://youtube.com")
- D type notification must open a specific page within the app. ("link": "AppName://ACTIVITYNAME")

## Basic Template

Basic Template is the basic push notification received on apps.
<br/>(Expanded and unexpanded example)<br/><br/>

<img src="https://github.com/CleverTap/PushTemplates/blob/0.0.4/screens/basic%20color.png" width="300" />

Basic Template Keys | Required | Description 
 ---:|:---:|:---| 
 title | Required | Title 
 message | Required | Message 
 messageBody | Optional | Message line when Notification is expanded
 image | Optional | Image in url
 which | Optional | Value - `P`/`B`/`L`/`D`
 link | Required if 'which' is entered | url for 'which' type

### Example data format to send for basic template push notifications

```json
{
  "to": "/topics/Appname",
  "data": {
    "title": "Basic Template",
    "message": "You can use this format for basic template push notifications",
    "image": "https://img.youtube.com/vi/1N_zzi2ad04/hqdefault.jpg",
    "link": "https://www.youtube.com/watch?v=ZpuY57qrZs8",
    "which": "L"
  }
}
```

## Rating Template

Rating template lets your users give you feedback, this feedback is captured and if five starts is clicked then [playstore in-app review](https://developer.android.com/guide/playcore/in-app-review) is displayed inside the app.<br/>(Expanded and unexpanded example)<br/>

<img src="https://github.com/CleverTap/PushTemplates/blob/0.0.4/screens/rating.gif" width="300" />

Rating Template Keys | Required | Description 
 ---:|:---:|:--- 
 notificationType | Required  | Value - `R`
 title | Required | Title 
 message | Required | Message 
 messageBody | Optional | Message line when Notification is expanded
 image | Required | Image in url
 which | Optional | Value - `P`/`B`/`L`/`D`
 link | Required if 'which' is entered | url for 'which' type
 title_clr | Optional | Title Color in HEX (default - #A6A6A6)
 message_clr | Optional | Message Color in HEX (default - #A6A6A6)
 
**Note :** Maximum number of lines for message in collapsed view - 1

### Example data format to send for rating template push notifications
```json
{
  "to": "/topics/Appname",
  "data": {
    "notificationType": "R",
    "title": "Rating Template",
    "message": "You can use this format for rating template push notifications",
    "image": "https://img.youtube.com/vi/1N_zzi2ad04/hqdefault.jpg",
    "link": "https://www.youtube.com/watch?v=ZpuY57qrZs8",
    "which": "L"
  }
}
```

## Bezel Template

The Bezel template ensures that the background image covers the entire available surface area of the push notification. All the text is overlayed on the image.<br/>(Expanded and unexpanded example)<br/>

### Zero Bezel Template

<img src="https://github.com/CleverTap/PushTemplates/blob/0.0.4/screens/zerobezel.gif" width="300" />

Zero Bezel Template Keys | Required | Description 
 ---:|:---:|:--- 
 notificationType | Required  | Value - `Z`
 title | Required | Title 
 message | Required | Message 
 messageBody | Optional | Message line when Notification is expanded
 image | Required | Image in url
 which | Optional | Value - `P`/`B`/`L`/`D`
 link | Required if 'which' is entered | url for 'which' type
 meta_clr | Optional | Color for appname,timestamp in HEX (default - #A6A6A6)
 title_clr | Optional | Title Color in HEX (default - ##000000)
 message_clr | Optional | Message Color in HEX (default - #000000)
 pt_bg | Optional | Background Color in HEX (default - #FFFFFF)
 
 **Note :** Maximum number of lines for message in collapsed view - 2
 
 ### Example data format to send for zero bezel template push notifications
```json
{
  "to": "/topics/Appname",
  "data": {
    "notificationType": "Z",
    "title": "Zero Bezel Template",
    "message": "You can use this format for zero bezel template push notifications",
    "image": "https://img.youtube.com/vi/1N_zzi2ad04/hqdefault.jpg",
    "link": "https://www.youtube.com/watch?v=ZpuY57qrZs8",
    "which": "L"
  }
}
```

### One Bezel Template

<img src="https://user-images.githubusercontent.com/69451072/94699515-03c24200-0358-11eb-9b29-ec3d22e40890.png" width="300" />


 One Bezel Template Keys | Required | Description 
 ---:|:---:|:--- 
 notificationType | Required  | Value - `O`(Capital Letter O)
 title | Required | Title 
 message | Required | Message 
 messageBody | Optional | Message line when Notification is expanded
 image | Required | Image in url
 which | Optional | Value - `P`/`B`/`L`/`D`
 link | Required if 'which' is entered | url for 'which' type
 meta_clr | Optional | Color for appname,timestamp in HEX (default - #A6A6A6)
 title_clr | Optional | Title Color in HEX (default - ##000000)
 message_clr | Optional | Message Color in HEX (default - #000000)
 pt_bg | Optional | Background Color in HEX (default - #FFFFFF)
 
 **Note :** Maximum number of lines for message in collapsed view - 3
 
  
### Example data format to send for one bezel template push notifications

```json
{
  "to": "/topics/Appname",
  "data": {
    "notificationType": "O",
    "title": "One Bezel Template",
    "message": "You can use this format for one bezel template push notifications",
    "image": "https://img.youtube.com/vi/1N_zzi2ad04/hqdefault.jpg",
    "link": "https://www.youtube.com/watch?v=ZpuY57qrZs8",
    "which": "L"
  }
}
```

### Image With Heading Template

<img src="https://i.postimg.cc/J40NTq2v/Screenshot-from-2021-07-01-17-14-08.png" width="300" />


 Image With Heading Template Keys | Required | Description
 ---:|:---:|:---
 notificationType | Required  | Value - `imageWithHeading`
 title | Required | Title
 messageBody | Required | Message line when Notification is collapsed
 image | Required | Image in url
 which | Optional | Value - `P`/`B`/`L`/`D`
 link | Required if 'which' is entered | url for 'which' type
 meta_clr | Optional | Color for appname,timestamp in HEX (default - #F FFFFF)
 title_clr | Optional | Title Color in HEX (default - ##000000)
 message_clr | Optional | Message Color in HEX (default - #000000)
 pt_bg | Optional | Background Color in HEX (default - #FFFFFF)

 **Note :** Maximum number of lines for message in collapsed view - 2


### Example data format to send for Image With Heading template push notifications

```json
{
  "to": "/topics/Appname",
  "data": {
    "notificationType": "imageWithHeading",
    "title": "Image With Heading Template",
    "message": "You can use this format for Image With Heading template push notifications",
    "image": "https://img.youtube.com/vi/1N_zzi2ad04/hqdefault.jpg",
    "link": "https://www.youtube.com/watch?v=ZpuY57qrZs8",
    "which": "L"
  }
}
```

### Image With Sub Heading Template

<img src="https://i.postimg.cc/htbVv5zJ/Screenshot-from-2021-07-01-17-14-56.png" width="300" />


 Image With Sub Heading Template Keys | Required | Description
 ---:|:---:|:---
 notificationType | Required  | Value - `imageWithSubHeading`
 title | Required | Title
 message | Required | Message
 messageBody | Required | Message line when Notification is collapsed
 image | Required | Image in url
 which | Optional | Value - `P`/`B`/`L`/`D`
 link | Required if 'which' is entered | url for 'which' type
 title_clr | Optional | Title Color in HEX (default - ##000000)
 message_clr | Optional | Message Color in HEX (default - #000000)
 pt_bg | Optional | Background Color in HEX (default - #FFFFFF)

 **Note :** Maximum number of lines for message in collapsed view - 2


### Example data format to send for Image With Sub Heading template push notifications

```json
{
  "to": "/topics/Appname",
  "data": {
    "notificationType": "imageWithSubHeading",
    "title": "Image With Heading Template",
    "message": "You can use this format for Image With Heading template push notifications",
    "image": "https://img.youtube.com/vi/1N_zzi2ad04/hqdefault.jpg",
    "link": "https://www.youtube.com/watch?v=ZpuY57qrZs8",
    "which": "L"
  }
}
```

### Small Text Image Card Template

<img src="https://i.postimg.cc/g2QnK9wt/Screenshot-from-2021-07-01-17-15-41.png" width="300" />


 Small Text Image Card Template Keys | Required | Description
 ---:|:---:|:---
 notificationType | Required  | Value - `smallTextImageCard`
 title | Required | Title
 message | Required | Message
 messageBody | Required | Message line when Notification is collapsed
 image | Required | Image in url
 which | Optional | Value - `P`/`B`/`L`/`D`
 link | Required if 'which' is entered | url for 'which' type
  meta_clr | Optional | Color for appname,timestamp in HEX (default - #000000)
 title_clr | Optional | Title Color in HEX (default - ##000000)
 message_clr | Optional | Message Color in HEX (default - #000000)
 pt_bg | Optional | Background Color in HEX (default - #FFFFFF)

 **Note :** Maximum number of lines for message in collapsed view - 2


### Example data format to send for Small Text Image Card template push notifications

```json
{
  "to": "/topics/Appname",
  "data": {
    "notificationType": "smallTextImageCard",
    "title": "Image With Heading Template",
    "message": "You can use this format for Image With Heading template push notifications",
    "image": "https://img.youtube.com/vi/1N_zzi2ad04/hqdefault.jpg",
    "link": "https://www.youtube.com/watch?v=ZpuY57qrZs8",
    "which": "L"
  }
}
```

# Functions

[(Back to top)](#table-of-contents)

## Check for Notifications

1.Call *checkForNotifications* method in your MainActivity to recieve data from notifications
```Kotlin
checkForNotifications(context: Context, intent: Intent, webViewActivity: Class<out Activity?>?,activityToOpen: Class<out Activity?>?,intentParam: String)
```
**Note:**

1.All the parameters are required.

2.Empty string - `""` should be given as default value for intentParam.

3.You can get the url of the link from getIntent() in activity using key - 'link'.

4.You can see the logs of the library using tag - 'FirebaseMessageService'

### Example
```Kotlin
MyFirebaseMessagingService myFirebaseMessagingService = new MyFirebaseMessagingService();
MyFirebaseMessaging.checkForNotifications(context = this, intent = intent, webViewActivity = WebViewActivity::class.java, activityToOpen = MainActivity::class.java,"")
```

## Subscribe to Topics

1.Call *addTopics* method in your MainActivity to subscribe topics for push notifications.
```Kotlin
addTopics(context: Context, appName: String, isDebug: Boolean)
```
**Note:**

1.All the parameters are required.

2.Empty String - "" should be given as a default value for appName so that your application name in lowercase is taken in library. 

3.Name format of the topic subscribed for release variant - 'appName','appName-country-language'( Ex - 'WhatsApp-IN-en' )

4.Name format of the topic subscribed for debug varaint - 'appNameDebug' ( Ex - 'WhatsAppDebug' )

### Example
```Kotlin
 MyFirebaseMessagingService myFirebaseMessagingService = new MyFirebaseMessagingService();
 myFirebaseMessagingService.addTopics(context = this,appName = appName, isDebug = BuildConfig.DEBUG)
```

# InApp Notifications

[(Back to top)](#push-notifications--inApp-notifications)

# Table of contents

- [Installation](#installation)
- [Firebase](#firebase)
- [CleverTap](#cleverTap)

# Installation

[(Back to top)](#table-of-contents-1)

1.To import this library, Add the following line to your project's *build.gradle* at the end of repositories.
```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}

```
2.To import this library, Add the following line to your app level *build.gradle* file.
```groovy
implementation implementation 'com.github.Appyhigh:push-notification-utils:1.3.5'

```

3.If you are using **Firebase** for InApp Notifications, then add the following lines to your app level *build.gradle* file.
```groovy
implementation 'com.google.firebase:firebase-inappmessaging-display:19.1.1' (#Recommended latest version)
```

4.If you are using **CleverTap** for InApp Notifications, then add the following lines to your app level *build.gradle* file.
```groovy
implementation 'com.clevertap.android:clevertap-android-sdk:4.0.4' (#Recommended latest version)
implementation 'androidx.fragment:fragment:1.1.0'
```


3.Add the following line to your *AndroidManifest.xml* for internet permission.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

```

4.If you are using **CleverTap** for InApp Notifications, then add the following lines to your *AndroidManifest.xml* file.

```xml
 <meta-data
    android:name="CLEVERTAP_ACCOUNT_ID"
    android:value="**your_accountId**"/>
<meta-data
    android:name="CLEVERTAP_TOKEN"
    android:value="**your_token**"/>
<activity
    android:name="com.clevertap.android.sdk.InAppNotificationActivity"
    android:theme="@android:style/Theme.Translucent.NoTitleBar"
    android:configChanges="orientation|keyboardHidden"/>

<meta-data
    android:name="CLEVERTAP_INAPP_EXCLUDE"
    android:value="**YourSplashActivity1**, **YourSplashActivity2**" /> 
```

We have 4 types of InApp notifications P, B, L and D which are identified by the which parameter.
- P type notifications must open the play store. ("link": "com.appyhigh")
- B type notifications must open the default browser. ("link": "https://youtube.com")
- L type notifications must open the webview within the app. ("link": "https://youtube.com")
- D type notification must open a specific page within the app. ("link": "AppName://ACTIVITYNAME")

# Firebase

[(Back to top)](#table-of-contents-1)

1.Create a campaign in Firebase InApp Messaging console.

**Note:** For more information, visit [Firebase docs for InApp Messaging](https://firebase.google.com/docs/in-app-messaging)

2.For action urls 
	- Enter deeplinks of the app to open a particular activity inside the app.
	- Enter urls to redirect to playstore or browser.
	
**Note:**

1.For Deep Links, do not forget to add the following lines inside the *activity* of *AndroidManifest.xml*.
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <data
	android:host="your domain link which you created in firebase console**"
	android:scheme="https"/>
</intent-filter>
```

# CleverTap

[(Back to top)](#table-of-contents-1)

1.Create a mobile-inApp campaign in CleverTap console,

**Note:** For more information, visit [CleverTap docs for InApp Notifications](https://developer.clevertap.com/docs/android#section-in-app-notifications)

2.For Buttons, enter the custom key-value pairs.

Custom Keys | Required | Description 
 ---:|:---:|:--- 
 which | Optional | Value - `P`/`B`/`L`/`D`
 link | Required if 'which' is entered | url for 'which' type
 title | Optional | title to pass for webViewActivity
 
3.To recieve InApp notifications, follow any of the two methods

### Import the inAppButtonClickListener from Library
1.Call the *setListener* method in the activity to handle the inApp Notification

```Kotlin
setListener(context: Context, webViewActivityToOpen: Class<out Activity?>?, activityToOpen: Class<out Activity?>?, intentParam: String)

```
**Note:**

1.All the parameters are required.

2.Empty string - `""` should be given as default value for intentParam.

#### Example
```Java
MyFirebaseMessagingService myFirebaseMessagingService = new MyFirebaseMessagingService();
myFirebaseMessagingService.setListener(context = this, webViewActivity = WebViewActivity::class.java, activityToOpen = MainActivity::class.java,"");
```
### Implement the inAppButtonClickListener explicitly without importing it from library.

1.Make sure your activity implements the InAppNotificationButtonListener and override the following method
```Java
public class MainActivity extends AppCompatActivity implements InAppNotificationButtonListener {
	@Override
	public void onInAppButtonClick(HashMap<String, String> hashMap) {
	  if(hashMap != null){
	    //Read the values
	  }
	}
}
```

2.Convert the recieved hashmap to Bundle in the above *if condition*.

3.Set the InAppNotificationButtonListener using the following code in your activity.
```Java
CleverTapAPI.getDefaultInstance(context).setInAppNotificationButtonListener(this);

```

3.Call the *checkForInAppNotifications* method inside the *onInAppButtonClick* method to handle the recieved data.
```Kotlin
checkForInAppNotifications(context: Context, extras: Bundle, webViewActivity: Class<out Activity?>?,activityToOpen: Class<out Activity?>?,intentParam: String)
```

#### Example
```Kotlin
class MainActivity : AppCompatActivity(), InAppNotificationButtonListener {
	override fun onInAppButtonClick(hashMap: HashMap<String?, String?>) 
	{
		val extras = Bundle()
		for ((key, value) in hashMap.entries) {
		    extras.putString(key, value)
		    Log.d("extras", "-> $extras")
		}
		checkForInAppNotifications(this, extras, WebViewActivityToOpen, activityToOpen, intentParam)
    	}
	
	override fun onCreate(savedInstanceState: Bundle?) 
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		CleverTapAPI.getDefaultInstance(context)!!.setInAppNotificationButtonListener(this)
	}
    }
}

```
