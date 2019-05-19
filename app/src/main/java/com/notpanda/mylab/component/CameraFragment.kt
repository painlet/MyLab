package com.notpanda.mylab.component

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.notpanda.mylab.R
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * https://developer.android.com/guide/components/fragments
 */
class CameraFragment : Fragment() {
    var photo_count = 0
    var camera: Camera? = null
    var lastPhotoPath = ""
    var newPhotoPath = ""

    private var cameraPreview: CameraPreview? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    @SuppressLint("MissingSuperCall")
    override fun onAttach(context: Context?) {
        super.onAttach(activity)
        Log.i(TAG, "onAttach")
    }

    override fun onDetach() {
        super.onDetach()
        Log.i(TAG, "onDetach")
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        openCamera()
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "onStart")
    }

    override fun onPause() {
        Log.i(TAG, "onPause")
        super.onPause()
        camera?.stopPreview()
        releaseCamera()
    }

    fun takePhoto(): String {
        Log.i(TAG, "takePhoto. camera=[$camera]")
        if (camera == null) {
            Log.e(TAG, "re-open camera as it is empty")
            openCamera()
        }
        camera!!.takePicture(null, null, this.mPicture)
        for (i in 0..20) {
            if (newPhotoPath.isEmpty() || newPhotoPath.equals(lastPhotoPath)) {
                Thread.sleep(1000)
                Log.d(TAG, "waiting for photo $i. $lastPhotoPath|$newPhotoPath")
            }
        }
        Log.i(TAG, "$photo_count: $newPhotoPath")
        lastPhotoPath = newPhotoPath
        camera!!.startPreview()
        return lastPhotoPath
    }

    fun stopPreview(){
        camera!!.stopPreview()
    }

    private fun openCamera() {
        Log.i(TAG, "openCamera.")
        camera = getCameraInstance()
        cameraPreview = CameraPreview(this.activity!!, camera!!)
        Log.i(TAG, "cameraPreview. $cameraPreview")
        Log.i(TAG, "view. ${this.view}")
        val preview = this.activity!!.findViewById(R.id.camera_layout) as FrameLayout
        Log.i(TAG, "preview. $preview")
        preview!!.addView(cameraPreview)
        Log.i(TAG, "openCamera. fin")
    }

    /** A safe way to get an instance of the Camera object.  */
    private fun getCameraInstance(): Camera? {
        var c: Camera? = null
        try {
            c = Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
        }
        return c // returns null if camera is unavailable
    }

    private var mPicture: Camera.PictureCallback = Camera.PictureCallback { data, camera ->
        Log.i(TAG, "Picture callback")
        val pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE) ?: return@PictureCallback
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(pictureFile)
            outputStream.write(data)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Save exception = $e")
        } catch (e: IOException) {
            Log.e(TAG, "Save exception = $e")
        } finally {
            try {
                if (outputStream != null) {
                    outputStream!!.close()
                }
            } catch (e: Exception) {
                Log.v(TAG, "Save exception = $e")
            }
            showToast("Camera capture has completed " + pictureFile)
            val filePath = pictureFile.toString()
            Log.i(TAG, "filePath=$filePath")
            newPhotoPath = filePath
        }
    }

    /** Create a File for saving an image or video  */
    private fun getOutputMediaFile(type: Int): File? {
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), SUB_FOLDER_NAME
        )
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory")
                return null
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val mediaFile: File
        mediaFile = File(
            mediaStorageDir.path + File.separator +
                    "IMG_" + timeStamp + ".jpg"
        )
        return mediaFile
    }

    /** Check if this device has a camera  */
    private fun checkCameraHardware(context: Context): Boolean {
        var value = false
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            value = true
        }
        return value
    }

    private fun releaseCamera() {
        camera?.release() // release the recorder object
        camera = null
    }

    private fun showToast(text: String) {
        activity?.runOnUiThread { Toast.makeText(activity, text, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        private val SUB_FOLDER_NAME = "MyLab"
        private val TAG = CameraFragment::class.java.simpleName
        private const val MEDIA_TYPE_IMAGE = 1
        fun newInstance(): CameraFragment {
            return CameraFragment()
        }
    }
}
