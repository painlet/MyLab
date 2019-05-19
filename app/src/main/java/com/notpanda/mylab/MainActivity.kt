package com.notpanda.mylab

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import com.notpanda.mylab.component.CameraFragment
import com.notpanda.mylab.component.LabHttpServer
import com.notpanda.mylab.utils.Utils
import java.io.IOException

class MainActivity : AppCompatActivity() {
    lateinit var cameraFragment: CameraFragment
    private var server: LabHttpServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //
        val infoView = findViewById(R.id.info_view) as TextView
        val infoStr = Utils.getIPAddress()
        infoView.setText(infoStr)
        Log.i(TAG, "IP:$infoStr")

        //
        if (savedInstanceState == null) {
            cameraFragment = CameraFragment.newInstance()
            //This is a scheduled task
            supportFragmentManager.beginTransaction()
                .add(R.id.camera_layout, cameraFragment)
                .commit()
        }
    }

    public override fun onResume() {
        super.onResume()
        try {
            val assets = getAssets()
            server = LabHttpServer(this)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun takePhoto(): String {
        Log.i(TAG, "takePhoto")
        return cameraFragment.takePhoto()
    }

    companion object {
        private val TAG = this.javaClass.simpleName
    }
}
