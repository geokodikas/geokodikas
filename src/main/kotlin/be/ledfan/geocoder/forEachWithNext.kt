package be.ledfan.geocoder

fun <T> Collection<T>.forEachWithNext(f: (T, T) -> Unit) {

    zipWithNext().forEach { f(it.first, it.second) }

}