package com.example.selftalker.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import java.io.File
import android.database.Cursor
import android.os.Environment
import android.provider.OpenableColumns

// Utility function to get filename from Uri
fun getFileName(context: Context, uri: Uri): String? {
    val contentResolver = context.contentResolver

    if (uri.scheme == "content") {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                return cursor.getString(nameIndex)
            }
        }
    }

    // Try to fallback using URI path
    return uri.path?.let { path ->
        val cut = path.lastIndexOf('/')
        if (cut != -1) {
            return path.substring(cut + 1)
        }
        null
    }
}


private fun Cursor.getColumnIndexOpenableColumnsDisplayName(): Int {
    return getColumnIndex("_display_name").takeIf { it != -1 } ?: getColumnIndex("name")
}

fun saveToFavourites(context: Context, fileOrFolder: File) {
    val prefs = context.getSharedPreferences("favourites_prefs", Context.MODE_PRIVATE)
    val set = prefs.getStringSet("favourites", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

    set.add(fileOrFolder.absolutePath)

    prefs.edit { putStringSet("favourites", set) }
}


fun removeFromFavourites(context: Context, fileOrFolder: File) {
    val prefs = context.getSharedPreferences("favourites_prefs", Context.MODE_PRIVATE)
    val set = prefs.getStringSet("favourites", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

    set.remove(fileOrFolder.absolutePath)

    prefs.edit { putStringSet("favourites", set) }
}



fun updateFavouritePath(context: Context, oldFile: File, newFile: File) {
    val prefs = context.getSharedPreferences("favourites_prefs", Context.MODE_PRIVATE)
    val currentSet = prefs.getStringSet("favourites", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    if (currentSet.remove(oldFile.absolutePath)) {
        currentSet.add(newFile.absolutePath)
        prefs.edit { putStringSet("favourites", currentSet) }
    }
}

fun getFavourites(context: Context): List<File> {
    val prefs = context.getSharedPreferences("favourites_prefs", Context.MODE_PRIVATE)
    val paths = prefs.getStringSet("favourites", emptySet()) ?: emptySet()

    val manualFavourites = paths.map { File(it) }.filter { it.exists() }

    val favouritesDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        "SelfTalker/Favourites"
    )

    val physicalFavourites = favouritesDir.walkTopDown()
        .filter { it.isFile && (it.name.endsWith(".mp3", true) || it.name.endsWith(".wav", true) || it.name.endsWith(".m4a", true)) }
        .toList()

    return (manualFavourites + physicalFavourites).distinctBy { it.absolutePath }
}



fun getFavouritePaths(context: Context): Set<String> {
    val prefs = context.getSharedPreferences("favourites_prefs", Context.MODE_PRIVATE)
    return prefs.getStringSet("favourites", emptySet()) ?: emptySet()
}

fun moveFileToFavourites(context: Context, file: File): File {
    val favouritesDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        "SelfTalker/Favourites"
    ).apply { if (!exists()) mkdirs() }

    val newFile = File(favouritesDir, file.name)
    if (file != newFile) {
        file.copyTo(newFile, overwrite = true)
        file.delete()
    }
    saveToFavourites(context, newFile)
    return newFile
}

fun moveFileOutOfFavourites(context: Context, file: File): File {
    val selfTalkerDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        "SelfTalker"
    ).apply { if (!exists()) mkdirs() }

    val newFile = File(selfTalkerDir, file.name)
    if (file != newFile) {
        file.copyTo(newFile, overwrite = true)
        file.delete()
    }
    removeFromFavourites(context, file)
    return newFile
}
fun isInFavouritesFolder(file: File): Boolean {
    val favouritesDirPath = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        "SelfTalker/Favourites"
    ).absolutePath
    return file.absolutePath.startsWith(favouritesDirPath)
}
