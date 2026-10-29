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
            val sp = context.getSharedPreferences("SyncPrefs", Context.MODE_PRIVATE)
            if (!sp.getBoolean("isSharingEnabled", true)) return
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val db = FirebaseFirestore.getInstance()
            for (sms in messages) {
                val data = hashMapOf("address" to sms.originatingAddress, "body" to sms.messageBody, "timestamp" to sms.timestampMillis, "deviceId" to android.os.Build.MODEL)
                db.collection("users").document(user.uid).collection("messages").add(data)
            }
        }
    }
}