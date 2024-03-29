package com.appyhigh.pushNotifications.apiclient

import com.appyhigh.pushNotifications.models.NotificationPayloadModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface APIInterface {

    @GET("notifications")
    fun getNotifications(
        @Header("Authorization") authorization: String?,
        @Query("package_id") packageName: String?
    ): Call<ArrayList<NotificationPayloadModel>>
}