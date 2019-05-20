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
import java.util.*


class LabHttpServer @Throws(IOException::class)
constructor(mainActivity: MainActivity) : NanoHTTPD(PORT) {
    var mMainActivity: MainActivity

    init {
        start()
        mMainActivity = mainActivity
        Log.i(TAG, "\nRunning! Visit home page to http://${Utils.getIPAddress()}:$PORT/ \n")
    }




    val assets = mainActivity.assets

    private val showPhotoLambda = { session: IHTTPSession ->
        val filePath = session.parameters.get("file")!![0].toString()
        showImage(filePath)
    }

    private val shotLambda = { session: IHTTPSession ->
        val photoFilePath = mainActivity.takePhoto()
        newFixedLengthResponse("photo is taken: <br><hr><img width=\"480px\" src=\"/photo?file=$photoFilePath\" alt=\"photo should be here\">\n")
    }

    private val webArtifactsLambda = { session: IHTTPSession ->
        var path = Uri.parse(session.uri).path
        readTextContentFromAssetsAsResponse(assets, path)
    }

    private val defaultLambda = { session: IHTTPSession ->
        var path = Uri.parse(session.uri).path
        newFixedLengthResponse(Status.NOT_FOUND, "text/html", "File Not Found : $path")
    }

    private val mainpageLambda = { session: IHTTPSession ->
        readTextContentFromAssetsAsResponse(assets, "web/index.htm")
    }

    private val listPhotoLambda = { session: IHTTPSession ->
        val fileList = mainActivity.cameraFragment.getPhotoList()
        var sb = StringBuffer()
        for (filepath in fileList) {
            val file = File(filepath)
            Log.i(TAG, "file : ${file}")
            sb.append(
                "<tr><td><a href=\"/photo?file=${file.name}\">${file.name}</a></td><td>${file.length()}</td><td>>${Date(
                    file.lastModified()
                )}</td></tr>\n"
            )
        }
        val html = readTextContentFromAssets(assets, filePath = "web/photo_list.htm")
            .replace("#PHOTO_LIST_HTML#", sb.toString())
        newFixedLengthResponse(html)
    }

    private val routeMap = mapOf(
        "GET::/photo" to showPhotoLambda,
        "GET::/shot" to shotLambda,
        "GET::/photos" to listPhotoLambda
    )

    private fun readTextContentFromAssets(assets: AssetManager, filePath: String): String {
        var filePathInAsset = filePath
        if (filePathInAsset.startsWith("/")) {
            filePathInAsset = filePathInAsset.substring(1)
        }
        Log.d(TAG, "readTextContentFromAssetsAsResponse. $filePath -> $filePathInAsset")
        val sb = StringBuffer()
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(
                InputStreamReader(assets.open(filePathInAsset), "UTF-8")
            )
            var mLine = reader.readLine()
            //Log.d(TAG, mLine)
            while (mLine != null) {
                Log.d(TAG, mLine)
                sb.append(mLine)
                mLine = reader.readLine()
            }
        } catch (e: IOException) {
            Log.e(TAG, e.message)
            throw IllegalArgumentException("File Not Found : $filePath")
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

    //
    private fun readTextContentFromAssetsAsResponse(assets: AssetManager, filePath: String): Response {
        try {
            return newFixedLengthResponse(readTextContentFromAssets(assets, filePath))
        } catch (e: IllegalArgumentException) {
            return newFixedLengthResponse(Status.NOT_FOUND, "text/html", e.message)
        }
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
        val folder = mMainActivity.cameraFragment.getPhotoFolder()
        var realFilePath = filePath
        if (!realFilePath.startsWith(folder.absolutePath)) {
            realFilePath = "${folder}/${filePath}"
        }
        Log.i(TAG, realFilePath)
        var fis: FileInputStream? = null
        try {
            val file = File(realFilePath)
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