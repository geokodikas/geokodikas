package be.ledfan.geocoder

import kotlin.system.measureTimeMillis

inline fun <R> measureTimeMillisAndReturn(block: () -> R): Pair<Long, R> {
    var r: R? = null

    val time = measureTimeMillis {
        r = block()
    }

    r?.let {
        return Pair(time, it)
    } ?: throw Exception("Return value of measureTimeMillisAndReturn should not be null")

}