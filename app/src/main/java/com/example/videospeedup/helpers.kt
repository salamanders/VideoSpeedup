package com.example.videospeedup

import java.math.BigInteger

fun Long.toHumanReadableByteCountBin(): String = when {
    this == Long.MIN_VALUE || this < 0 -> "N/A"
    this < 1024L -> "$this B"
    this <= 0xfffccccccccccccL shr 40 -> "%.1f KiB".format(this.toDouble() / (0x1 shl 10))
    this <= 0xfffccccccccccccL shr 30 -> "%.1f MiB".format(this.toDouble() / (0x1 shl 20))
    this <= 0xfffccccccccccccL shr 20 -> "%.1f GiB".format(this.toDouble() / (0x1 shl 30))
    this <= 0xfffccccccccccccL shr 10 -> "%.1f TiB".format(this.toDouble() / (0x1 shl 40))
    this <= 0xfffccccccccccccL -> "%.1f PiB".format((this shr 10).toDouble() / (0x1 shl 40))
    else -> "%.1f EiB".format((this shr 20).toDouble() / (0x1 shl 40))
}


fun Int.pow(exp: Int): Int = BigInteger.valueOf(this.toLong()).pow(exp).toInt()