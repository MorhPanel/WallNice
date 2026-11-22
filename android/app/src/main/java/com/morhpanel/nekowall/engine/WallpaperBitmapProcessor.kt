package com.morhpanel.nekowall.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.morhpanel.nekowall.data.models.WallpaperFitMode
import kotlin.math.max
import kotlin.math.min

object WallpaperBitmapProcessor {
    fun processForDevice(context: Context, source: Bitmap, mode: WallpaperFitMode): Bitmap {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        return process(source, width, height, mode)
    }

    fun process(source: Bitmap, width: Int, height: Int, mode: WallpaperFitMode): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (mode) {
            WallpaperFitMode.CROP -> drawCrop(canvas, source, width, height, paint)
            WallpaperFitMode.FIT -> drawFit(canvas, source, width, height, paint)
            WallpaperFitMode.BLUR -> {
                val background = createSoftBackground(source, width, height)
                canvas.drawBitmap(background, null, Rect(0, 0, width, height), paint)
                if (background !== source) background.recycle()
                drawFit(canvas, source, width, height, paint)
            }
        }
        return output
    }

    private fun drawCrop(canvas: Canvas, source: Bitmap, width: Int, height: Int, paint: Paint) {
        val scale = max(width.toFloat() / source.width, height.toFloat() / source.height)
        val scaledWidth = source.width * scale
        val scaledHeight = source.height * scale
        val left = (width - scaledWidth) / 2f
        val top = (height - scaledHeight) / 2f
        canvas.drawBitmap(source, null, RectF(left, top, left + scaledWidth, top + scaledHeight), paint)
    }

    private fun drawFit(canvas: Canvas, source: Bitmap, width: Int, height: Int, paint: Paint) {
        val scale = min(width.toFloat() / source.width, height.toFloat() / source.height)
        val scaledWidth = source.width * scale
        val scaledHeight = source.height * scale
        val left = (width - scaledWidth) / 2f
        val top = (height - scaledHeight) / 2f
        canvas.drawBitmap(source, null, RectF(left, top, left + scaledWidth, top + scaledHeight), paint)
    }

    private fun createSoftBackground(source: Bitmap, width: Int, height: Int): Bitmap {
        val sampleWidth = (width / 24).coerceAtLeast(32)
        val sampleHeight = (height / 24).coerceAtLeast(32)
        val tiny = Bitmap.createBitmap(sampleWidth, sampleHeight, Bitmap.Config.ARGB_8888)
        val tinyCanvas = Canvas(tiny)
        drawCrop(
            tinyCanvas,
            source,
            sampleWidth,
            sampleHeight,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        val softened = Bitmap.createScaledBitmap(tiny, width, height, true)
        tiny.recycle()
        return softened
    }
}
