package com.notpanda.mylab.component

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.notpanda.mylab.MainActivity
import java.io.IOException

class CameraPreview(context: Context, private val mCamera: Camera) : SurfaceView(context), SurfaceHolder.Callback {
    private var mHolder: SurfaceHolder = holder
    var rotationInt: Int = 0

    init {
        mHolder.addCallback(this)
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(holder: SurfaceHolder) =
        try {
            mCamera.setPreviewDisplay(holder)
            this.setCameraDisplayOrientation()
            mCamera.startPreview()
        } catch (e: IOException) {
        }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // empty. Take care of releasing the Camera preview in your activity.
        Log.i(TAG, "surfaceDestroyed")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        Log.i(TAG, "surfaceChanged")
        if (mHolder.surface == null) {
            return
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.message)
        }
        try {
            Log.i(TAG, "surface changed width = $w and height = $h")
            //for (cameraSize in mCamera.parameters.supportedPreviewSizes) {
            //Log.i(TAG,"surface changed get preview sizes w = ${cameraSize.width},  ${cameraSize.height}")
            //}
            mCamera.setPreviewDisplay(mHolder)
            mCamera.startPreview()
        } catch (e: Exception) {
            Log.i(TAG, "Error starting camera preview: " + e.message)
        }
    }

    private fun setCameraDisplayOrientation() {
        Log.i(TAG, "setCameraDisplayOrientation")
        val info = android.hardware.Camera.CameraInfo()
        android.hardware.Camera.getCameraInfo(this.getCameraID(), info)
        val activity = context as MainActivity
        val rotation = activity.windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }

        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        Log.i(TAG, "setCameraDisplayOrientation = $result")
        mCamera.setDisplayOrientation(result)
        val params = mCamera.parameters
        params.setRotation(result)
        mCamera.parameters = params
        rotationInt = result
    }

    fun getCameraID(): Int {
        Log.i(TAG, "getCameraID")
        var cameraId = 0
        for (i in 0..Camera.getNumberOfCameras()) {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i
                Log.i(TAG, "Camera found with camera ID = $cameraId");
                break
            }
        }
        return cameraId
    }

    companion object {
        private val TAG = this::class.java.simpleName
    }
}