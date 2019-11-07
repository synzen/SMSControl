package com.example.testapplication

import android.app.IntentService
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.Telephony
import android.util.Log

// TODO: Rename actions, choose action names that describe tasks that this
// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
private const val FETCH_SMS = "com.example.testapplication.action.FETCH_SMS"
private const val ACTION_BAZ = "com.example.testapplication.action.BAZ"

// TODO: Rename parameters
private const val EXTRA_PARAM1 = "com.example.testapplication.extra.PARAM1"
private const val EXTRA_PARAM2 = "com.example.testapplication.extra.PARAM2"

var FETCHING_SMS = false

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class FetchMessages : IntentService("FetchMessages") {

    fun getSMS (): String {
        val cursor = contentResolver.query(Uri.parse("content://sms"), null, null, null, null)
        var msgData = ""
        var sendProgress = 0
        var i = 0

        if (cursor?.moveToFirst() == true) { // must check the result to prevent exception
            val total = cursor.count
            do {
                for (idx in 0 until cursor.columnCount) {
//                    Log.d("TAG", "continuing this column ${cursor.getColumnName(idx)} ${idx}/${cursor.columnCount}")
                    msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx)
                }
                var floatProgress = (i++.toFloat()/total) * 100
                var intProgress = floatProgress.toInt()
                Log.d("TAG", "continuing cursor ${i}/${total}, ${floatProgress} ${intProgress}")
                if (intProgress > sendProgress) {
                    sendProgress = intProgress
                    sendBroadcast(Intent(SMS_FETCHING_UPDATE_ACTION).apply {
                        putExtra(SMS_FETCHING_UPDATE_ACTION, sendProgress)
                    })
                }



                // use msgData
            } while (cursor.moveToNext())
        } else {
            // empty box, no SMS
            Log.d("TAG", "no sms!")
        }
        cursor?.close()
        Log.d("TAG", msgData)
        return msgData
    }

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            FETCH_SMS -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM1)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
                handleActionFoo(param1, param2)
            }
            ACTION_BAZ -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM1)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
                handleActionBaz(param1, param2)
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionFoo(param1: String, param2: String) {
        if (FETCHING_SMS) {
            Log.d("TAG", "already running fetch sms")
            return
        }
        FETCHING_SMS = true
        Log.d("TAG", "Running get sms now.")
        getSMS()
        sendBroadcast(Intent(SMS_FETCHED_ACTION))
        FETCHING_SMS = false
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionBaz(param1: String, param2: String) {
        TODO("Handle action Baz")
    }

    companion object {
        @JvmStatic
        var running = false

        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionFoo(context: Context, param1: String, param2: String) {
            val intent = Intent(context, FetchMessages::class.java).apply {
                action = FETCH_SMS
                putExtra(EXTRA_PARAM1, param1)
                putExtra(EXTRA_PARAM2, param2)
            }
            Log.d("TAG", "starting action")
            context.startService(intent)
        }

        /**
         * Starts this service to perform action Baz with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionBaz(context: Context, param1: String, param2: String) {
            val intent = Intent(context, FetchMessages::class.java).apply {
                action = ACTION_BAZ
                putExtra(EXTRA_PARAM1, param1)
                putExtra(EXTRA_PARAM2, param2)
            }
            context.startService(intent)
        }
    }
}
