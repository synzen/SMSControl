package com.example.testapplication

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.provider.Settings.Secure
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.Telephony.Sms
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



const val CHANGE_SMS_DEFAULT_RESULT = 2


class DisplayMessageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_message)

        val message = intent.getStringExtra(EXTRA_MESSAGE)
        val textView = findViewById<TextView>(R.id.textView).apply {
            text = message
        }

        var btn = findViewById<Button>(R.id.button2)
        btn.setOnClickListener {
            Log.d("tag", "clicked")
//            contentResolver.delete(Telephony.Sms.CONTENT_URI, null, null)
            contentResolver.delete(Sms.CONTENT_URI, Sms._ID + "!=?", arrayOf("0"))
//            if (FETCHING_SMS) {
//                Log.d("TAG", "fetching sms already taking place")
//            } else if (getDefaultSmsPackageName() != packageName) {
//                Log.d("AA", "nope")
//                Log.d("TAG", "here" + getDefaultSmsPackageName())
//                val setSmsAppIntent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
//                setSmsAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
//                startActivity(setSmsAppIntent)
//
//
//                Log.d("TAG", "not default")
//            } else if (checkPermission()) {
//                FetchSMS.startActionFoo(this, "", "")
//            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            CHANGE_SMS_DEFAULT_RESULT -> {
                Log.d("AG", "Default successful")
            }
        }
    }

    private fun checkPermission() : Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            Log.d("TAG", "Already got permPERM!")
            return true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), PERM_CODE_READ_SMS)
            return false
        }
    }

    private fun getDefaultSmsPackageName() : String {
        return Telephony.Sms.getDefaultSmsPackage(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERM_CODE_READ_SMS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TAG", "OK")
                }
                checkPermission()
            }
        }
    }
}
