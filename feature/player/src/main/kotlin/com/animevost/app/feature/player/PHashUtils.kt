package com.animevost.app.feature.player

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

object PHashUtils {
    private const val SIZE = 32
    private const val SMALL = 8

    fun compute(source: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(source, SIZE, SIZE, true)
        val pixels = FloatArray(SIZE * SIZE)
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val c = scaled.getPixel(x, y)
                pixels[y * SIZE + x] =
                    0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)
            }
        }
        if (scaled != source) scaled.recycle()

        // 2D DCT, top-left 8×8 block
        val dct = Array(SMALL) { v ->
            FloatArray(SMALL) { u ->
                var sum = 0f
                val cu = if (u == 0) 1f / sqrt(2f) else 1f
                val cv = if (v == 0) 1f / sqrt(2f) else 1f
                for (y in 0 until SIZE) {
                    for (x in 0 until SIZE) {
                        sum += pixels[y * SIZE + x] *
                            cos((2 * x + 1) * u * PI / (2.0 * SIZE)).toFloat() *
                            cos((2 * y + 1) * v * PI / (2.0 * SIZE)).toFloat()
                    }
                }
                (2f / SIZE) * cu * cv * sum
            }
        }

        // Collect 63 AC components (skip DC at 0,0)
        val coeffs = mutableListOf<Float>()
        for (v in 0 until SMALL) for (u in 0 until SMALL) {
            if (u == 0 && v == 0) continue
            coeffs.add(dct[v][u])
        }
        val median = coeffs.sorted()[coeffs.size / 2]

        var hash = 0L
        for ((i, c) in coeffs.withIndex()) {
            if (c > median) hash = hash or (1L shl i)
        }
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
