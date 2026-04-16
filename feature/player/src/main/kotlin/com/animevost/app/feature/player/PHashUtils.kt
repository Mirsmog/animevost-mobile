package com.animevost.app.feature.player

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos

object PHashUtils {

    private const val SIZE = 32
    private const val DCT_SIZE = 8

    fun compute(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, true)
        val gray = Array(SIZE) { y ->
            FloatArray(SIZE) { x ->
                val pixel = scaled.getPixel(x, y)
                0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(pixel)
            }
        }
        if (scaled !== bitmap) scaled.recycle()

        // 8×8 DCT on the top-left 8×8 block
        val dct = FloatArray(DCT_SIZE * DCT_SIZE)
        for (u in 0 until DCT_SIZE) {
            for (v in 0 until DCT_SIZE) {
                var sum = 0.0
                for (x in 0 until DCT_SIZE) {
                    for (y in 0 until DCT_SIZE) {
                        sum += gray[y][x] *
                            cos(Math.PI * (2 * x + 1) * u / (2.0 * DCT_SIZE)) *
                            cos(Math.PI * (2 * y + 1) * v / (2.0 * DCT_SIZE))
                    }
                }
                val cu = if (u == 0) 1.0 / Math.sqrt(2.0) else 1.0
                val cv = if (v == 0) 1.0 / Math.sqrt(2.0) else 1.0
                dct[u * DCT_SIZE + v] = (0.25 * cu * cv * sum).toFloat()
            }
        }

        // Exclude DC component (0,0), compute mean of remaining 63 values
        var mean = 0.0
        for (i in 1 until DCT_SIZE * DCT_SIZE) {
            mean += dct[i]
        }
        mean /= (DCT_SIZE * DCT_SIZE - 1)

        // Build 64-bit hash
        var hash = 0L
        for (i in 0 until DCT_SIZE * DCT_SIZE) {
            if (i == 0) continue // skip DC
            if (dct[i] > mean) {
                hash = hash or (1L shl i)
            }
        }
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = (a xor b).countOneBits()
}
