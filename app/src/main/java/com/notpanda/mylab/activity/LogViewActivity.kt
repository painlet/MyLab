package com.notpanda.mylab.activity

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.notpanda.mylab.R

class LogViewActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            Log.i(TAG, "initializing LogViewActivity")
            val process = Runtime.getRuntime().exec("logcat -d")
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream)
            )
            val log = StringBuilder()
            var line = bufferedReader.readLine()
            while (line != null) {
                line = bufferedReader.readLine()
                log.append(line)
            }
            //val tv = findViewById<TextView>(R.id.)
            //tv.text = log.toString()
        } catch (e: IOException) {
            Log.e(TAG, e.message)
        }
    }

    companion object {
        private val TAG = "LogViewActivity"
    }

}