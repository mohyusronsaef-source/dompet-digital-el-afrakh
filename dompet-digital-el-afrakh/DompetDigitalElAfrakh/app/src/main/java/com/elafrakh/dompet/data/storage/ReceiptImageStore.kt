package com.elafrakh.dompet.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ReceiptImageStore(private val context: Context) {

    private val appContext = context.applicationContext

    val receiptsDir: File
        get() = File(appContext.filesDir, "receipts").apply {
            if (!exists()) mkdirs()
        }

    private val cameraTempDir: File
        get() = File(appContext.cacheDir, "camera_temp").apply {
            if (!exists()) mkdirs()
        }

    fun createTempCameraUri(): Pair<Uri, File> {
        val tempFile = File(cameraTempDir, "temp_nota_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            tempFile
        )
        return uri to tempFile
    }

    suspend fun saveCompressedReceiptFromUri(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val sampledBitmap = appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return@withContext null

            val orientedBitmap = applyExifOrientationIfNeeded(sourceUri, sampledBitmap)
            val scaledBitmap = scaleDownIfNeeded(orientedBitmap, MAX_DIMENSION_PX)
            if (scaledBitmap !== orientedBitmap && !orientedBitmap.isRecycled) {
                orientedBitmap.recycle()
            }

            val fileName = "nota_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val destFile = File(receiptsDir, fileName)
            FileOutputStream(destFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (!scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
            fileName
        }.getOrNull()
    }

    suspend fun loadBitmap(fileName: String, maxSizePx: Int = 1024): Bitmap? = withContext(Dispatchers.IO) {
        val file = getReceiptFile(fileName)
        if (!file.exists()) return@withContext null
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSizePx)
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()
    }

    fun getReceiptFile(fileName: String): File = File(receiptsDir, fileName)

    fun deleteReceiptFile(fileName: String?) {
        if (fileName.isNullOrBlank()) return
        val file = getReceiptFile(fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    fun clearAllReceipts() {
        receiptsDir.listFiles()?.forEach { it.delete() }
        cameraTempDir.listFiles()?.forEach { it.delete() }
    }

    private fun applyExifOrientationIfNeeded(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / maxSide.toFloat()
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        val maxSide = maxOf(width, height)
        while (maxSide / (inSampleSize * 2) >= maxDim) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    companion object {
        private const val MAX_DIMENSION_PX = 1280
        private const val JPEG_QUALITY = 80
    }
}
