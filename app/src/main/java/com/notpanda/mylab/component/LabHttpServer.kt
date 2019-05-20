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
        showImage(filePath)
    }

    private val shotLambda = { session: IHTTPSession ->
        val photoFilePath = mainActivity.takePhoto()
        newFixedLengthResponse("photo is taken: <br><hr><img width=\"480px\" src=\"/image?file=$photoFilePath\" alt=\"photo should be here\">\n")
    }

    private val webArtifactsLambda = { session: IHTTPSession ->
        var path = Uri.parse(session.uri).path
        readTextContentFromAssets(assets, path)
    }

    private val defaultLambda = { session: IHTTPSession ->
        var path = Uri.parse(session.uri).path
        newFixedLengthResponse(Status.NOT_FOUND, "text/html", "File Not Found : $path")
    }

    private val mainpageLambda = { session: IHTTPSession ->
        readTextContentFromAssets(assets, "web/index.htm")
    }

    private val stopPreviewLambda = { session: IHTTPSession ->
        mainActivity.cameraFragment.stopPreview()
        newFixedLengthResponse("stopped preview")
    }

    private fun readTextContentFromAssets(assets: AssetManager, filePath: String): Response {
        var filePathInAsset = filePath
        if(filePathInAsset.startsWith("/")){
            filePathInAsset = filePathInAsset.substring(1)
        }
        Log.d(TAG, "readTextContentFromAssets. $filePath -> $filePathInAsset")
        val sb = StringBuffer()
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(
                InputStreamReader(assets.open(filePathInAsset), "UTF-8")
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
            return newFixedLengthResponse(Status.NOT_FOUND, "text/html", "File Not Found : $filePath")
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    Log.e(TAG, e.message)
                }
            }
        }
        return newFixedLengthResponse(sb.toString())
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

        if (uri.path.equals("/")
            || uri.path.equals("/index")
            || uri.path.equals("/index.html")
        ) {
            return route(session, mainpageLambda)
        }

        if (uri.path.startsWith("/web/")) {
            return route(session, webArtifactsLambda)
        }

        if (routeMap.containsKey(routePath)) {
            return route(session, routeMap.get(routePath)!!);
        }
        return route(session, defaultLambda)
    }

    private fun route(session: IHTTPSession, handler: (session: IHTTPSession) -> Response): Response {
        Log.i(TAG, "route request to -> ${handler.javaClass.canonicalName}")
        return handler(session)
    }

    private fun showImage(filePath: String): Response {
        Log.i(TAG, "image file Path =[$filePath]")
        var fis: FileInputStream? = null
        try {
            val file = File(filePath)
            fis = FileInputStream(file)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return newFixedLengthResponse(Status.NOT_FOUND, "text/html", "File Not Found : $filePath")
        }
        val res = newChunkedResponse(
            Status.OK
            , "image/jpeg", fis
        )
        return res
    }


    companion object {
        val TAG = this.javaClass.canonicalName
        private const val PORT = 8080
    }
}