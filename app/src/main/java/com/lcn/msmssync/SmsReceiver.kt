package com.lcn.msmssync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val sharedPreferences = context.getSharedPreferences("SyncPrefs", Context.MODE_PRIVATE)
            val isSharingEnabled = sharedPreferences.getBoolean("isSharingEnabled", true)
            
            if (!isSharingEnabled) return 

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null && messages != null) {
                val db = FirebaseFirestore.getInstance()
                val userId = currentUser.uid

                for (sms in messages) {
                    val smsData = hashMapOf(
                        "address" to sms.originatingAddress,
                        "body" to sms.messageBody,
                        "timestamp" to sms.timestampMillis,
                        "deviceId" to android.os.Build.MODEL
                    )

                    db.collection("users").document(userId)
                      .collection("messages")
                      .add(smsData)
                }
            }
        }
    }
}
