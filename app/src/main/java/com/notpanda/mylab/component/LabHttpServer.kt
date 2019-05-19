package com.notpanda.mylab.component

import android.content.res.AssetManager
import android.net.Uri
import android.util.Log
import com.notpanda.mylab.MainActivity
import com.notpanda.mylab.utils.Utils
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.response.Response

import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Response.newChunkedResponse
import java.io.*
import org.nanohttpd.protocols.http.response.Status


class LabHttpServer @Throws(IOException::class)
constructor(mainActivity: MainActivity) : NanoHTTPD(PORT) {
    val assets = mainActivity.assets

    private val imageLambda = { session: IHTTPSession ->
        val filePath = session.parameters.get("file")!![0].toString()
        //showImage("/storage/emulated/0/Pictures/MyLab/$filePath")
        showImage(filePath)
    }

    private val shotLambda = { session: IHTTPSession ->
        val photoFilePath = mainActivity.takePhoto()
        newFixedLengthResponse("photo is taken: <br><hr><img width=\"480px\" src=\"/image?file=$photoFilePath\" alt=\"photo should be here\">\n")
    }

    private val defaultLambda = { session: IHTTPSession ->
        newFixedLengthResponse(readHtmlFromAssets(assets, "index.htm"))

    }

    private val stopPreviewLambda= { session: IHTTPSession ->
        mainActivity.cameraFragment.stopPreview()
        newFixedLengthResponse("stopped preview")
    }

    private fun readHtmlFromAssets(assets: AssetManager, filePath: String): String {
        val sb = StringBuffer()
        var reader: BufferedReader? = null
        try {
            Log.d(TAG, "assets : ${assets.list("").joinToString(",")}")
            reader = BufferedReader(
                InputStreamReader(assets.open(filePath), "UTF-8")
            )
            var mLine = reader.readLine()
            Log.d(TAG, mLine)
            while (mLine != null) {
                Log.d(TAG, mLine)
                sb.append(mLine)
                mLine = reader.readLine()
            }
        } catch (e: IOException) {
            Log.e(TAG, e.message)
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    Log.e(TAG, e.message)
                }
            }
        }
        return sb.toString()
    }

    private val routeMap = mapOf(
        "GET::/image" to imageLambda,
        "GET::/shot" to shotLambda,
        "GET::/stoppreview" to stopPreviewLambda
    )

    init {
        start()
        Log.i(TAG, "\nRunning! Visit home page to http://${Utils.getIPAddress()}:$PORT/ \n")
    }

    public override fun serve(session: IHTTPSession): Response {
        var uri = Uri.parse(session.uri)
        val routePath = "${session.method.toString().toUpperCase()}::${uri.path}"
        Log.d(TAG, "routePath = $routePath")
        if (routeMap.containsKey(routePath)) {
            return route(session, routeMap.get(routePath)!!);
        }
        return route(session, defaultLambda)
    }

    private fun route(session: IHTTPSession, handler: (session: IHTTPSession) -> Response): Response {
        return handler(session)
    }

    /*
    fun test(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        val method = session.method
        val uri = session.uri
        if (Method.PUT == method || Method.POST == method) {
            try {
                session.parseBody(files)
            } catch (ioe: IOException) {
                return newFixedLengthResponse(
                    Status.INTERNAL_ERROR,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "SERVER INTERNAL ERROR: IOException: " + ioe.message
                )
            } catch (re: NanoHTTPD.ResponseException) {
                return newFixedLengthResponse(re.status, NanoHTTPD.MIME_PLAINTEXT, re.message)
            }

        }
        // get the POST body
        val postBody = session.queryParameterString
        // or you can access the POST request's parameters
        val postParameter = session.parms["parameter"]
        return newFixedLengthResponse(postBody) // Or postParameter.
    }
    */

    fun showImage(filePath: String): Response {
        Log.i(TAG, "image file Path =[$filePath]")
        var fis: FileInputStream? = null
        try {
            val file = File(filePath) //path exists and its correct
            fis = FileInputStream(file)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }


        val res = newChunkedResponse(
            Status.OK
            , "image/jpeg", fis
        )
        //res.addHeader("Content-Disposition", "attachment; filename=\"" + f.getName() + "\"")
        return res
    }


    companion object {
        private val TAG = "LabHttpServer"
        private val PORT = 8080

    }
}