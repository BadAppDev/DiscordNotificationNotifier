package com.example.discordnotificationnotifier

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.discordnotificationnotifier.ui.theme.DiscordNotificationNotifierTheme
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!isNotificationServiceEnabled()) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }

        setContent {
            DiscordNotificationNotifierTheme {
                WebhookManagerUI()
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val cn = ComponentName(this, NotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }
}

@Composable
fun WebhookManagerUI() {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("webhook_prefs", Context.MODE_PRIVATE)

    var webhookUrl by remember { mutableStateOf(sharedPrefs.getString("webhook_url", "") ?: "") }
    var inputText by remember { mutableStateOf(webhookUrl) }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter Discord Webhook URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            webhookUrl = inputText
            sharedPrefs.edit {
                putString("webhook_url", webhookUrl)
            }
            message = "Webhook URL updated!"
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Update Webhook")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (webhookUrl.isNotBlank()) {
                sendTestToDiscord(webhookUrl) {
                    message = it
                }
            } else {
                message = "Webhook URL is empty."
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Send Test")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = message)
    }
}


fun sendTestToDiscord(webhookUrl: String, onComplete: (String) -> Unit) {
    val json = JSONObject().put("content", "Test")
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder().url(webhookUrl).post(body).build()
    val client = OkHttpClient()

    CoroutineScope(Dispatchers.IO).launch {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onComplete("Failed to send test: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                onComplete("Test sent! Code: ${response.code}")
            }
        })
    }
}
