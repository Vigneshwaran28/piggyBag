package com.titanbag.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val body = message.messageBody
                if (body != null) {
                    processSms(context, body)
                }
            }
        }
    }

    private fun processSms(context: Context, body: String) {
        val db = AppDatabase.getDatabase(context)
        val repository = FinanceRepository(db)
        
        CoroutineScope(Dispatchers.IO).launch {
            repository.processIncomingMessage(body)
        }
    }
}
