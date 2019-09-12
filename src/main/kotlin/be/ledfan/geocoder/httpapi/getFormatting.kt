package be.ledfan.geocoder.httpapi

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpHeaders
import io.ktor.http.parseHeaderValue
import io.ktor.request.header


// TODO move to extension of call
fun getFormatting(call: ApplicationCall): String {
    val urlFormatting = call.request.queryParameters["formatting"]
    if (urlFormatting != null && listOf("json", "html").contains(urlFormatting)) {
        return urlFormatting
    }
    val headerFormatting = getFormattingFromAcceptHeader(call)
    if (headerFormatting != null) {
        return headerFormatting
    }
    return "json"
}

private fun getFormattingFromAcceptHeader(call: ApplicationCall): String? {
    val acceptHeader = parseHeaderValue(call.request.header(HttpHeaders.Accept))

    for (acceptValue in acceptHeader) {
        if (acceptValue.value == "application/json") {
            return "json"
        } else if (acceptValue.value == "text/html") {
            return "html"
        }
    }

    return null
}