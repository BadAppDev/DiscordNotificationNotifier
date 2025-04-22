package com.example.discordnotificationnotifier

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        if (packageName != "com.discord") {
            sendToDiscord(packageName)
        }
    }

    private fun sendToDiscord(appName: String) {
        val prefs = getSharedPreferences("webhook_prefs", MODE_PRIVATE)
        val webhookUrl = prefs.getString("webhook_url", null)

        if (webhookUrl.isNullOrBlank()) {
            Log.w("NotificationListener", "Webhook URL is not set.")
            return
        }

        val json = JSONObject().put("content", "Notification from app: $appName")
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(webhookUrl).post(body).build()
        val client = OkHttpClient()

        // Perform network operations on a background thread (using CoroutineScope)
        CoroutineScope(Dispatchers.IO).launch {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("NotificationListener", "Failed to send webhook", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.i("NotificationListener", "Webhook sent, code: ${response.code}")
                }
            })
        }
    }
}
