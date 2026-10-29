package com.lcn.msmssync

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SyncManager {
    fun syncLocalSmsToCloud(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val userId = currentUser.uid
        
        val uri = Uri.parse("content://sms/inbox")
        val cursor = context.contentResolver.query(uri, null, null, null, "date DESC")

        cursor?.use {
            val addressIndex = it.getColumnIndex("address")
            val bodyIndex = it.getColumnIndex("body")
            val dateIndex = it.getColumnIndex("date")

            var count = 0
            while (it.moveToNext() && count < 50) { 
                val address = it.getString(addressIndex)
                val body = it.getString(bodyIndex)
                val date = it.getLong(dateIndex)

                val smsData = hashMapOf(
                    "address" to address,
                    "body" to body,
                    "timestamp" to date,
                    "deviceId" to android.os.Build.MODEL
                )

                db.collection("users").document(userId)
                  .collection("messages").document(date.toString())
                  .set(smsData)
                
                count++
            }
        }
    }
}
