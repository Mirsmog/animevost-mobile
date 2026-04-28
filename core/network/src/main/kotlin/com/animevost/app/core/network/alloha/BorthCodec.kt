package com.animevost.app.core.network.alloha

import java.security.MessageDigest

/**
 * Pure-Kotlin port of the obfuscated Alloha player Borth header generator.
 *
 * Reverse-engineered from `app.cf1b9efb.js` (functions LY/Lw/LG/Lp). The Alloha
 * server issues a seed via `<meta name="viewporti" content="...">`, scrambled
 * by three permutations. The client must reverse the permutation chain and
 * send the result as base64 alongside a fingerprint sha256 in the `Borth`
 * header. After every successful POST the server emits a fresh `Borth`
 * response header that is used as the next seed.
 *
 * The fingerprint hash (`Lj`) is server-validated only by format, not value,
 * so we use a fixed Chrome-on-Android profile to keep things deterministic.
 */
object BorthCodec {

    /** Pre-computed `sha256(fingerprintParts.join("||"))` for a stable Chrome-Android profile. */
    val FINGERPRINT_HASH: String by lazy { computeFingerprintHash() }

    private val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 7.0; SM-G892A Build/NRD90M; wv) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Version/4.0 Chrome/109.0.0.0 Mobile Safari/537.36"

    /** Build the value of the `Borth` request header from the current seed. */
    fun buildBorthHeader(seed: String): String {
        val payload = lG(lW(lY(seed)))
        return "$FINGERPRINT_HASH|$payload"
    }

    /** Browser User-Agent that pairs with the precomputed fingerprint. */
    fun userAgent(): String = USER_AGENT

    // ------------------------------------------------------------------ permutations

    /** Inverse of `LY` — distributes characters by `bitLength(index)` buckets, top-down. */
    internal fun lY(input: String): String {
        val n = input.length
        if (n <= 1) return input
        var bits = 0
        while ((1 shl bits) < n) bits++
        val counts = IntArray(bits + 1)
        for (i in 0 until n) counts[bitLength(i)]++
        val groups = arrayOfNulls<String>(bits + 1)
        var off = 0
        for (i in bits downTo 0) {
            groups[i] = input.substring(off, off + counts[i])
            off += counts[i]
        }
        val ptr = IntArray(bits + 1)
        val out = CharArray(n)
        for (i in 0 until n) {
            val g = bitLength(i)
            out[i] = groups[g]!![ptr[g]++]
        }
        return String(out)
    }

    /** Inverse of `Lw` — distributes by trailing-zero count. */
    internal fun lW(input: String): String {
        val n = input.length
        if (n <= 1) return input
        var bits = 0
        while ((1 shl bits) < n) bits++
        val counts = IntArray(bits + 1)
        for (i in 0 until n) counts[trailingZeros(i, bits)]++
        val groups = arrayOfNulls<String>(bits + 1)
        var off = 0
        for (i in 0..bits) {
            groups[i] = input.substring(off, off + counts[i])
            off += counts[i]
        }
        val ptr = IntArray(bits + 1)
        val out = CharArray(n)
        for (i in 0 until n) {
            val g = trailingZeros(i, bits)
            out[i] = groups[g]!![ptr[g]++]
        }
        return String(out)
    }

    /** Inverse of `LG` — coprime-2 ring shuffle. */
    internal fun lG(input: String): String {
        val n = input.length
        if (n <= 1) return input
        val prime = nextPrime(maxOf(2, n + 1))
        val used = BooleanArray(n)
        val order = IntArray(n)
        var orderSize = 0
        var idx = 0
        while (orderSize < n) {
            idx = (idx + 2) % prime
            if (idx < n && !used[idx]) {
                order[orderSize++] = idx
                used[idx] = true
            }
        }
        val out = CharArray(n)
        for (i in 0 until n) out[order[i]] = input[i]
        return String(out)
    }

    private fun bitLength(value: Int): Int {
        if (value == 0) return 0
        var v = value
        var c = 0
        while (v > 0) { c++; v = v ushr 1 }
        return c
    }

    private fun trailingZeros(value: Int, bitsCap: Int): Int {
        if (value == 0) return bitsCap
        var v = value
        var c = 0
        while ((v and 1) == 0) { c++; v = v ushr 1 }
        return c
    }

    private fun nextPrime(start: Int): Int {
        var p = start
        while (!isPrime(p)) p++
        return p
    }

    private fun isPrime(n: Int): Boolean {
        if (n < 2) return false
        if (n % 2 == 0) return n == 2
        var i = 3
        while (i.toLong() * i <= n) {
            if (n % i == 0) return false
            i += 2
        }
        return true
    }

    private fun computeFingerprintHash(): String {
        val parts = listOf(
            USER_AGENT,
            "Europe/Moscow",
            "1080x1920",
            "ru",
            "4",
            "4",
        )
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(parts.joinToString("||").toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
