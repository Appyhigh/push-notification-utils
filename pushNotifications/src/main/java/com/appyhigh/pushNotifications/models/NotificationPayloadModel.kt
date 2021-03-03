package com.appyhigh.pushNotifications.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NotificationPayloadModel(
    @SerializedName("id")
    @Expose
    var id: String = "",

    @SerializedName("data")
    @Expose
    var data: String = "",

)