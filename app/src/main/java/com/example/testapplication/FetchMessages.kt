package com.example.testapplication

import android.app.IntentService
import android.content.ContentValues
import android.content.Intent
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.Telephony
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

// TODO: Rename actions, choose action names that describe tasks that this
// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
private const val WRITE_SMS = "com.example.testapplication.action.WRITE_SMS"
private const val RESTORE_SMS = "com.example.testapplication.action.RESTORE_SMS"

// TODO: Rename parameters
private const val EXTRA_PARAM_FILE_PATH = "com.example.testapplication.extra.FILE_PATH"

var WRITING_SMS = false
var RESTORING_SMS = false
/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class FetchMessages : IntentService("FetchMessages") {
    private fun recordRows(cursor: Cursor, jsonWriter: JsonWriter, typeReference: Map<String, Int>) {
        val total = cursor.count
        var i = 0
        var sendProgress = 0
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
            val floatProgress = (i++.toFloat()/total) * 100
            val intProgress = floatProgress.toInt()
            jsonWriter.endObject()
            if (intProgress > sendProgress) {
                sendProgress = intProgress
                sendBroadcast(Intent(SMS_FETCHING_UPDATE_ACTION).apply {
                    putExtra(SMS_FETCHING_UPDATE_ACTION, sendProgress)
                })
            }
        } while (cursor.moveToNext())
    }

    private fun writeToFile(pathToWriteTo: Uri, fileName: String) {
        val rw = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        if (rw) {
            Log.d("TAG", "yes")
        } else {
            Log.d("TAG", "no")
        }
        if (pathToWriteTo == Uri.EMPTY) {
            Log.d("TAG", "Empty")
            return
        }

        val folderDocument = DocumentFile.fromTreeUri(this, pathToWriteTo)
        if (folderDocument == null) {
            Log.d("TAG", "folder does not exist")
            return
        }

        var foundFile = folderDocument.findFile("$fileName.json")
        if (foundFile == null) {
            foundFile = folderDocument.findFile(FILE_SMS_BACKUP) // Sometimes the json is not appended while writing
        }
        if (foundFile != null && !foundFile.delete()) {
            Log.d("TAG", "file already exists and was unable to be deleted")
            return
        }
        val fileURI = folderDocument.createFile("application/json", fileName)?.uri
        if (fileURI == null) {
            Log.d("TAG", "null file URI")
            return
        }
        Log.d("TAG", "file path is ${fileURI.path}")
        val outputStream = contentResolver.openOutputStream(fileURI)
        if (outputStream == null) {
            Log.d("TAG", "null output stream")
            return
        }
        val jsonWriter = JsonWriter(OutputStreamWriter(outputStream))
        val cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, null)
        if (cursor == null || !cursor.moveToFirst()) {
            Log.d("tag", "null cursor")
            return
        }
        val mmsCursor = contentResolver.query(Telephony.Mms.CONTENT_URI, null, null, null, null)
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
            if (mmsCursor != null && mmsCursor.moveToFirst()) {
                recordRows(mmsCursor, this, MMS_TABLE_TYPES)
            }
            mmsCursor?.close()
            endArray()
            endObject()
            flush()
            close()
        }
    }

    private fun readFile(path: Uri, fileName: String) {
        // Store the column types by column name
        if (path == Uri.EMPTY) {
            Log.d("TAG", "Empty path")
            return
        }

        // Open the file
        val dir = DocumentFile.fromTreeUri(this, path)
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

        //Log.d("TAG", "data: ${File("${file.uri.toS}.json").bufferedReader().readLines()}")
        val rows: MutableList<ContentValues> = mutableListOf()
        JsonReader(InputStreamReader(inputStream)).run {
            beginObject()
            nextName() // SMS
            beginArray()
            while (hasNext()) {
                beginObject()
                val row = ContentValues()
                while (hasNext()) {
                    val key = nextName()
                    if (!SMS_TABLE_TYPES.containsKey(key)) {
                        skipValue()
                        continue
                    }

                    if (SMS_TABLE_TYPES[key] == Cursor.FIELD_TYPE_INTEGER) {
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
            nextName() // MMS
            beginArray()
            endArray()
            endObject()
            close()
        }
        val total = rows.count()
        var i = 0
        for (row in rows) {
            Log.d("TAG", "Inserting ${++i}/$total")
            contentResolver.insert(Telephony.Sms.CONTENT_URI, row)
        }
        Log.d("TAG", "insertion complete")
    }

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            WRITE_SMS -> {
                val filePath = intent.getStringExtra(EXTRA_PARAM_FILE_PATH)
                handleActionFetchSMS(filePath)
            }
            RESTORE_SMS -> {
                val filePath = intent.getStringExtra(EXTRA_PARAM_FILE_PATH)
                handleActionRestoreSMS(filePath)
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionFetchSMS(filePath: String?) {
        if (WRITING_SMS) {
            Log.d("TAG", "already running fetch sms")
            return
        }
        WRITING_SMS = true
        Log.d("TAG", "Running get sms now.")
        writeToFile(Uri.parse(filePath), FILE_SMS_BACKUP)
        sendBroadcast(Intent(SMS_WRITTEN_ACTION))
        WRITING_SMS = false
    }

    private fun handleActionRestoreSMS(filePath: String?) {
        if (RESTORING_SMS) {
            Log.d("TAG", "already running fetch sms")
            return
        }
        RESTORING_SMS = true
        Log.d("TAG", "Running get sms now.")
//        writeToFile(Uri.parse(filePath), FILE_SMS_BACKUP)
        readFile(Uri.parse(filePath), FILE_SMS_BACKUP)
        sendBroadcast(Intent(SMS_RESTORED_ACTION))
        RESTORING_SMS = false
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
        fun startActionWriteSMS(context: Context, filePath: Uri) {
            val intent = Intent(context, FetchMessages::class.java).apply {
                action = WRITE_SMS
                putExtra(EXTRA_PARAM_FILE_PATH, filePath.toString())
            }
            Log.d("TAG", "starting write")
            context.startService(intent)
        }

        @JvmStatic
        fun startActionRestoreSMS(context: Context, filePath: Uri) {
            val intent = Intent(context, FetchMessages::class.java).apply {
                action = RESTORE_SMS
                putExtra(EXTRA_PARAM_FILE_PATH, filePath.toString())
            }
            Log.d("TAG", "starting restore")
            context.startService(intent)
        }
    }
}
