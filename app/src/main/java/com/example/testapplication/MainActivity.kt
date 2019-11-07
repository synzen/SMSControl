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
import android.os.Environment
import android.provider.Telephony
import android.util.*
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.documentfile.provider.DocumentFile.*
import java.io.*

const val FILE_SMS_BACKUP = "smsbackup"
const val FILE_CONV_BACKUP = "convbackup"
const val EXTRA_MESSAGE = "com.example.testapplication.MESSAGE"
const val PERM_CODE_READ_SMS = 1
const val SMS_FETCHED_ACTION = "com.example.testapplication.SMS_FETCHED"
const val SMS_FETCHING_UPDATE_ACTION = "com.example.testapplication.SMS_FETCHING"
const val ROLE_SMS_REQUEST_CODE = 2
const val READ_DIR_REQUEST_CODE = 3

val SMS_TABLE_TYPES = mapOf<String, Int>(
    Telephony.Sms._ID to Cursor.FIELD_TYPE_INTEGER,
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
                SMS_FETCHED_ACTION -> {
                    Log.d("TAG", "FETCHED ACTION COMPLETED")
                    progressBar.progress = 100
                }
                SMS_FETCHING_UPDATE_ACTION -> {
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
        var btn = findViewById<Button>(R.id.button3)
        var filter = IntentFilter()
        filter.addAction(SMS_FETCHED_ACTION)
        filter.addAction(SMS_FETCHING_UPDATE_ACTION)
        registerReceiver(broadcastReceiver, filter)
        btn.setOnClickListener {
            Log.d("TAG", "Sending change default intent")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val setSmsAppIntent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                setSmsAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                startActivityForResult(setSmsAppIntent, CHANGE_SMS_DEFAULT_RESULT)
            } else {
                val roleManager = getSystemService(RoleManager::class.java)
                if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {

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

    fun sendMessage(view: View) {
        // Do something in response to button
        Log.d("TAG", "going now")
        if (FETCHING_SMS) {
            Log.d("TAG", "fetching sms already taking place")
        } else if (getDefaultSmsPackageName() != packageName) {
            val setSmsAppIntent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            setSmsAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            startActivity(setSmsAppIntent)
            Log.d("TAG", "not default")
        } else {
            FetchMessages.startActionFoo(this, "", "")
        }
//        val intent = Intent(this, DisplayMessageActivity::class.java).apply {
//            putExtra(EXTRA_MESSAGE, msgData)
//        }
//        startActivity(intent)
    }

    fun cursorTest(view: View) {
        val cursor = contentResolver.query(Telephony.Mms.CONTENT_URI, null, null, null, null)
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
    }

    fun deleteMessages(view: View) {
        contentResolver.delete(Telephony.Sms.CONTENT_URI, null, null)
//        contentResolver.delete(Telephony.Sms.Conversations.CONTENT_URI, null, null)
    }

    fun selectDirectory(view: View) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, READ_DIR_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        Log.d("TAG", "$requestCode $resultCode")

        if (requestCode == ROLE_SMS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d("TAG", "GRANTED")
            } else {
                Log.d("TAG", "DENIED")
            }
        } else if (requestCode == READ_DIR_REQUEST_CODE) {
            data?.data?.also { uri ->
                Log.i("TAG", "uri: $uri")
                var uriDisplay = findViewById<TextView>(R.id.selectedUri)
                uriDisplay.text = uri.path
                pathToWriteTo = uri
            }
        }
    }

    fun beginWrite(view: View) {
        writeToFile(pathToWriteTo, FILE_SMS_BACKUP)
    }

    private fun recordRows(cursor: Cursor, jsonWriter: JsonWriter, typeReference: Map<String, Int>) {
        val total = cursor.count
        var i = 0
        do {
            jsonWriter.beginObject()
            for (idx in 0 until cursor.columnCount) {
                val columnName = cursor.getColumnName(idx)
                Log.d("TAG", "name: $columnName, value: ${cursor.getString(idx)}")
                if (!typeReference.containsKey(columnName)) {
                    continue
                }
                jsonWriter.name(columnName)
                if (typeReference[columnName] == Cursor.FIELD_TYPE_INTEGER) {
                    jsonWriter.value(cursor.getInt(idx))
                } else {
                    jsonWriter.value(cursor.getString(idx))
                }
            }
            Log.d("TAG", "${i++}/$total")
            jsonWriter.endObject()
        } while (cursor.moveToNext())
    }

    private fun writeToFile(path: Uri, fileName: String) {
//        try {
//            var osw = OutputStreamWriter(openFileOutput())
//        }
        var rw = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        if (rw) {
            Log.d("TAG", "yes")
        } else {
            Log.d("TAG", "no")
        }
        if (pathToWriteTo == Uri.EMPTY) {
            Log.d("TAG", "Empty")
            return
        }


        var folderDocument = fromTreeUri(this, path)
//        var files = folderDocument?.listFiles()
//        if (files != null) {
//            for (file in files) {
//                Log.d("D","file found: ${file.name}")
//            }
//        }
        if (folderDocument == null) {
            Log.d("TAG", "folder does not exist")
            return
        }
//        return

        var foundFile = folderDocument.findFile("$fileName.json")
        if (foundFile == null) {
            foundFile = folderDocument.findFile(FILE_SMS_BACKUP) // Sometimes the json is not appended while writing
        }
        if (foundFile != null && !foundFile.delete()) {
            Log.d("TAG", "file already exists and was unable to be deleted")
            return
        }
        var fileURI = folderDocument.createFile("application/json", fileName)?.uri
        if (fileURI == null) {
            Log.d("TAG", "null file URI")
            return
        }
        Log.d("TAG", "file path is ${fileURI.path}")
        var outputStream = contentResolver.openOutputStream(fileURI)
        if (outputStream == null) {
            Log.d("TAG", "null output stream")
            return
        }
        var jsonWriter = JsonWriter(OutputStreamWriter(outputStream))
        val cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, null)
        if (cursor == null || !cursor.moveToFirst()) {
            Log.d("tag", "null cursor")
            return
        }
        val mmsCursor = contentResolver.query(Telephony.Mms.CONTENT_URI, null, null, null, null)
        if (mmsCursor == null || !mmsCursor.moveToFirst()) {
            Log.d("TAG", "null mms cursor")
            return
        }
        Log.d("TAG", "write success")
        jsonWriter.run {
            beginObject()
            name("sms")
            beginArray()
            recordRows(cursor, this, SMS_TABLE_TYPES)
            cursor.close()
            endArray()
            name("mms")
            beginArray()
            recordRows(mmsCursor, this, MMS_TABLE_TYPES)
            endArray()
            endObject()
            flush()
            close()
        }
    }

    fun beginRead(view: View) {
        readFile(pathToWriteTo, FILE_SMS_BACKUP, Telephony.Sms.CONTENT_URI, SMS_TABLE_TYPES)
    }

    private fun readFile(path: Uri, fileName: String, contentUri: Uri, dataTypes: Map<String, Int>) {
        // Store the column types by column name
        if (!checkPermission()) {
            return
        }

        if (path == Uri.EMPTY) {
            Log.d("TAG", "Empty path")
            return
        }

        // Open the file
        val dir = fromTreeUri(this, path)
        if (dir == null) {
            Log.d("TAG", "document file null for read")
            return
        }

        val file: DocumentFile? = dir.findFile("$fileName.json") ?: dir.findFile(fileName)
        if (file == null) {
            Log.d("TAG", "No file found")
            return
        }
        val inputStream: InputStream? = contentResolver.openInputStream(file.uri)
        if (inputStream == null) {
            Log.d("TAG", "null input stream")
            return
        }
        var rows: MutableList<ContentValues> = mutableListOf()
        JsonReader(InputStreamReader(inputStream)).run {
            beginArray()
            while (hasNext()) {
                beginObject()
                val row = ContentValues()
                while (hasNext()) {
                    val key = nextName()
                    if (!dataTypes.containsKey(key)) {
                        skipValue()
                        continue
                    }

                    if (dataTypes[key] == Cursor.FIELD_TYPE_INTEGER) {
                        row.put(key, nextInt())
                    } else if (peek() == JsonToken.NULL) {
                        Log.d("TAG", "null token? $key")
                        nextNull()
                        row.putNull(key)
                    } else {
                        val value = nextString()
                        Log.d("TAG", "Putting ${key}:${value}")
                        row.put(key, value)
                    }
//                    Log.d("json", "key: ${nextName()}")
//                    Log.d("json", "value: ${nextString()}")
                }
                endObject()
                if (row.size() > 0) {
                    rows.add(row)
                }
            }
            endArray()
            close()
        }
        var total = rows.count()
        var i = 0
        for (row in rows) {
            Log.d("TAG", "Inserting ${i++}/$total")
            contentResolver.insert(contentUri, row)

        }
        Log.d("TAG", "insertion complete")
    }

}
