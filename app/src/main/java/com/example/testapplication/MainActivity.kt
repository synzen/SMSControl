package com.example.testapplication

import android.Manifest
import android.app.role.RoleManager
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Telephony
import android.util.*
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

const val FILE_SMS_BACKUP = "smsbackup"
const val FILE_CONV_BACKUP = "convbackup"
const val EXTRA_MESSAGE = "com.example.testapplication.MESSAGE"
const val PERM_CODE_READ_SMS = 1
const val SMS_WRITTEN_ACTION = "com.example.testapplication.SMS_FETCHED"
const val SMS_RESTORED_ACTION = "com.example.testapplication.SMS_RESTORED"
const val SMS_FETCHING_UPDATE_ACTION = "com.example.testapplication.SMS_FETCHING"
const val SMS_RESTORING_UPDATE_ACTION = "com.example.testapplication.SMS_RESTORING"
const val ROLE_SMS_REQUEST_CODE = 2
const val READ_DIR_REQUEST_CODE = 3

val SMS_TABLE_TYPES = mapOf<String, Int>(
//    Telephony.Sms._ID to Cursor.FIELD_TYPE_INTEGER,
//    Telephony.TextBasedSmsColumns.THREAD_ID to Cursor.FIELD_TYPE_INTEGER,
    Telephony.TextBasedSmsColumns.ADDRESS to Cursor.FIELD_TYPE_STRING,
    Telephony.TextBasedSmsColumns.DATE_SENT to Cursor.FIELD_TYPE_INTEGER,
    Telephony.TextBasedSmsColumns.DATE to Cursor.FIELD_TYPE_INTEGER,
    Telephony.TextBasedSmsColumns.BODY to Cursor.FIELD_TYPE_STRING,
    Telephony.TextBasedSmsColumns.CREATOR to Cursor.FIELD_TYPE_STRING,
    Telephony.TextBasedSmsColumns.PERSON to Cursor.FIELD_TYPE_INTEGER,
    Telephony.TextBasedSmsColumns.SUBJECT to Cursor.FIELD_TYPE_STRING,
    Telephony.TextBasedSmsColumns.SEEN to Cursor.FIELD_TYPE_INTEGER,
    Telephony.TextBasedSmsColumns.READ to Cursor.FIELD_TYPE_INTEGER,
    Telephony.TextBasedSmsColumns.TYPE to Cursor.FIELD_TYPE_INTEGER,
    Telephony.TextBasedSmsColumns.PROTOCOL to Cursor.FIELD_TYPE_INTEGER,
    Telephony.TextBasedSmsColumns.SERVICE_CENTER to Cursor.FIELD_TYPE_STRING,
    Telephony.TextBasedSmsColumns.REPLY_PATH_PRESENT to Cursor.FIELD_TYPE_INTEGER,
    Telephony.TextBasedSmsColumns.ERROR_CODE to Cursor.FIELD_TYPE_INTEGER,
    Telephony.TextBasedSmsColumns.LOCKED to Cursor.FIELD_TYPE_INTEGER
)

val MMS_TABLE_TYPES = mapOf<String, Int>(
)

class MainActivity : AppCompatActivity() {
    private var pathToWriteTo = Uri.EMPTY
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar2)
            when (intent?.action) {
                SMS_WRITTEN_ACTION -> {
                    Log.d("TAG", "FETCHED ACTION COMPLETED")
                    progressBar.progress = 100
                }
                SMS_RESTORED_ACTION -> {
                    Log.d("TAG", "RESTORED ACTION COMPLETED")
                    progressBar.progress = 100
                }
                SMS_FETCHING_UPDATE_ACTION, SMS_RESTORING_UPDATE_ACTION -> {
//                    var receieved = intent.getFloatExtra(SMS_FETCHING_UPDATE_ACTION, 1f)
//                    Log.d("TAG", "Received ${receieved}")
                    Thread(Runnable {
                        val progress = intent.getIntExtra(SMS_FETCHING_UPDATE_ACTION, 0)
                        Log.d("TAG", "got progress $progress")
                        progressBar.progress = progress
                    }).start()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.button3)
        val filter = IntentFilter()
        filter.addAction(SMS_WRITTEN_ACTION)
        filter.addAction(SMS_FETCHING_UPDATE_ACTION)
        registerReceiver(broadcastReceiver, filter)
        btn.setOnClickListener {
            Log.d("TAG", "Sending change default intent")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val setSmsAppIntent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                setSmsAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                /**
                 * TODO: Make sure this works
                 */
                startActivityForResult(setSmsAppIntent, ROLE_SMS_REQUEST_CODE)
            } else {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {

                    startActivityForResult(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS),
                        ROLE_SMS_REQUEST_CODE
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TAG", "unregistering")
        unregisterReceiver(broadcastReceiver)
    }

    private fun checkPermission() : Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            Log.d("TAG", "Already got permPERM!")
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), PERM_CODE_READ_SMS)
        }
        return hasPermission
    }

    private fun getDefaultSmsPackageName() : String {
        return Telephony.Sms.getDefaultSmsPackage(this)
    }

    fun sendMessage(@Suppress("UNUSED_PARAMETER")view: View) {
        // Do something in response to button
        Log.d("TAG", "going now")
//        val intent = Intent(this, DisplayMessageActivity::class.java).apply {
//            putExtra(EXTRA_MESSAGE, msgData)
//        }
//        startActivity(intent)
    }

    fun cursorTest(@Suppress("UNUSED_PARAMETER")view: View) {
        val cursor = contentResolver.query(Uri.parse("content://mms/part"), null, "mid=80", null, null)
        if (cursor == null || !cursor.moveToFirst()) {
            Log.d("TAG", "null cursor or no data")
            return
        }
        val total = cursor.count
        var i = 0
        do {
            for (idx in 0 until cursor.columnCount) {
                val columnName = cursor.getColumnName(idx)
                Log.d("TAG", "name: $columnName, value: ${cursor.getString(idx)}")
            }
            Log.d("TAG", "${i++}/$total")
        } while (cursor.moveToNext())
        cursor.close()
    }

    fun deleteMessages(@Suppress("UNUSED_PARAMETER")view: View) {
         contentResolver.delete(Telephony.Sms.CONTENT_URI, null, null)
    }

    fun selectDirectory(@Suppress("UNUSED_PARAMETER")view: View) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, READ_DIR_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ROLE_SMS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d("TAG", "GRANTED")
            } else {
                Log.d("TAG", "DENIED")
            }
        } else if (requestCode == READ_DIR_REQUEST_CODE) {
            data?.data?.also { uri ->
                Log.i("TAG", "uri: $uri")
                val uriDisplay = findViewById<TextView>(R.id.selectedUri)
                uriDisplay.text = uri.path
                pathToWriteTo = uri
            }
        }
    }

    fun beginWrite(@Suppress("UNUSED_PARAMETER")view: View) {
        if (WRITING_SMS) {
            Log.d("TAG", "fetching sms already taking place")
        } else if (!checkPermission()) {
            Log.d("TAG", "no permission, asking now")
        } else if (pathToWriteTo == Uri.EMPTY) {
            Log.d("TAG", "No path to write to")
        } else {
            FetchMessages.startActionWriteSMS(this, pathToWriteTo)
        }
    }

    fun beginRead(@Suppress("UNUSED_PARAMETER")view: View) {
        if (RESTORING_SMS) {
            Log.d("TAG", "reading sms already taking place")
        } else if (getDefaultSmsPackageName() != packageName) {
            val setSmsAppIntent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            setSmsAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            startActivity(setSmsAppIntent)
            Log.d("TAG", "not default")
        } else if (pathToWriteTo == Uri.EMPTY) {
            Log.d("TAG", "No path to write to")
        } else {
            FetchMessages.startActionRestoreSMS(this, pathToWriteTo)
        }
    }
}
