package com.taspen.taspenlife2

import android.app.Application
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val FILENAME_FORMAT = "dd-MMM-yyyy"

val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
        .format(Date())

fun createFile(application: Application): File {
    val mediaDir = application.externalMediaDirs.firstOrNull()?.let {
        File(it, application.resources.getString(R.string.app_name)).apply { mkdirs() }
    }
//    val mediaDir = Environment.getExternalStoragePublicDirectory(
//        Environment.DIRECTORY_PICTURES)

    val outputDirectory = if (
        mediaDir != null && mediaDir.exists()
    ) mediaDir else application.filesDir

    return File(outputDirectory, "$timeStamp.jpg")
}