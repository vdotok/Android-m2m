package com.vdotok.many2many.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.google.gson.Gson
import com.vdotok.streaming.models.SessionDataModel
import com.vdotok.many2many.prefs.Prefs
import com.vdotok.many2many.utils.ApplicationConstants.DOCS_DIRECTORY
import java.io.*


/**
 * Created By: VdoTok
 * Date & Time: On 1/20/21 At 3:31 PM in 2021
 *
 * File Utils class to write file information related functions
 * such as filePath, file conversion, temp file creation etc
 */

/**
 * Function to get the real path of a file using URI
 * @return Returns a string path of the file
 * */
fun getRealPathFromURI(context: Context?, contentUri: Uri?): String? {
    var cursor: Cursor? = null
    return try {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        cursor = context?.contentResolver?.query(contentUri!!, proj, null, null, null)
        val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        cursor.getString(column_index)
    } catch (e: java.lang.Exception) {
        Log.i("path", "getRealPathFromURI Exception : $e")
        ""
    } finally {
        cursor?.close()
    }
}

/**
 * Function to create a directory in external storage
 * @return Returns a String of absolute path of the created directory
 * */
fun createAppDirectory(): String? {
    val dir: File? = File(Environment.getExternalStorageDirectory().absolutePath + DOCS_DIRECTORY)

    if (dir != null) {
        if (!dir.exists()) dir.mkdirs()
    }

    return dir?.absolutePath
}

fun writeCallLogToFile(callLogs: SessionDataModel) {
    val sdMain = File(Environment.getExternalStorageDirectory().absolutePath + DOCS_DIRECTORY)
    val dest = File(sdMain, ApplicationConstants.CALL_LOGS_FILE_NAME + ".txt")
    try {
        // response is the data written to file
//            PrintWriter(dest).use { out -> out.appendLine("Mango") }
        val gson = Gson()
        val callLogObject = gson.toJson(callLogs)
        dest.appendText("\nJSON:\n$callLogObject")
    } catch (e: Exception) {
        Log.e("Call_Logs", "${e.message}")
    }
}

/**
 * Function to save Image on External storage
 * @param filePath String file path of the image
 * @return Returns a File after storing to the external location
 * */

fun saveFileDataOnExternalData(filePath: String) {
    try {
        val f = File(filePath)
        if (!f.exists())
            f.createNewFile()
        else
            Log.e("Call_Logs", "File Already Exists!")
        // File Saved
    } catch (e: FileNotFoundException) {
        println("FileNotFoundException")
        e.printStackTrace()
    } catch (e: IOException) {
        println("IOException")
        e.printStackTrace()
    }
}

//for android 9 and lower
fun makeFileInStorage() {
    val fileName = ApplicationConstants.CALL_LOGS_FILE_NAME + ".txt"
    val filePath = createAppDirectory() + "/$fileName"
    saveFileDataOnExternalData(filePath)
}

fun downloadFile(context: Context, callLogs: SessionDataModel) {
    var prefs = Prefs(context)

    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, "VdotTok_Call_logs" + ".txt")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }

    val resolver = context.contentResolver

    //Storing at primary location
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        TODO("VERSION.SDK_INT < Q")
    }
    val file = File(prefs.fileInfo)
    if (!file.exists()) {
        val item = resolver.insert(collection, values)

        val parcelFileDescriptor =
            context.contentResolver?.openFileDescriptor(item!!, "w", null)

        val fileOutputStream = FileOutputStream(parcelFileDescriptor!!.fileDescriptor)
        val gson = Gson()
        val callLogObject = gson.toJson(callLogs)
        fileOutputStream.write(callLogObject.toByteArray())
        fileOutputStream.close()
        val fileS = File(copyFileToInternalStorage(context, item!!, "s"))
        prefs.fileInfo = fileS.path


        values.clear()

        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        item.let { resolver.update(it, values, null, null) }

    } else {
        val files = File(prefs.fileInfo)
        val gson = Gson()
        val callLogObject = gson.toJson(callLogs)
        files.appendText("\nJSON:\n$callLogObject")
    }
}

fun copyFileToInternalStorage(
    context: Context,
    uri: Uri,
    newDirName: String
): String? {
    val returnUri = uri
    val returnCursor: Cursor? = context.contentResolver?.query(
        returnUri, arrayOf(
            OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
        ), null, null, null
    )

    /*
 * Get the column indexes of the data in the Cursor,
 *     * move to the first row in the Cursor, get the data,
 *     * and display it.
 * */
    val nameIndex: Int? = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    returnCursor?.moveToFirst()
    val name: String? = returnCursor?.getString(nameIndex!!)
    val output: File = if (newDirName != "") {
        val dir = File(context.filesDir.toString() + "/" + newDirName)
        if (!dir.exists()) {
            dir.mkdir()
        }
        File(context.filesDir.toString() + "/" + newDirName + "/" + name)
    } else {
        File(context.filesDir.toString() + "/" + name)
    }
    try {
        val inputStream: InputStream? = context.contentResolver?.openInputStream(uri)
        val outputStream = FileOutputStream(output)
        var read: Int
        val bufferSize = 1024
        val buffers = ByteArray(bufferSize)
        while (inputStream?.read(buffers).also { read = it!! } != -1) {
            outputStream.write(buffers, 0, read)
        }
        inputStream?.close()
        outputStream.close()
    } catch (e: java.lang.Exception) {
    }
    return output.path

}