package be.ledfan.geocoder

private val regex = "\\s+".toRegex()

fun String.trimDup(): String {
    return replace(regex, " ").trim()
}
