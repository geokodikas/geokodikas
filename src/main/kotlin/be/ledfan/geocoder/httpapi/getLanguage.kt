package be.ledfan.geocoder.httpapi

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpHeaders
import io.ktor.http.parseHeaderValue
import io.ktor.request.acceptLanguageItems
import io.ktor.request.header
import java.util.*
import kotlin.collections.LinkedHashSet


// TODO move to extension of call
fun getLanguage(call: ApplicationCall): LinkedHashSet<String> {
    val res = LinkedHashSet<String>()

    for (language in call.request.acceptLanguageItems()) {
        // only use the language and not the country, because that is what OSM is using
        res.add(Locale.forLanguageTag(language.value).language)
    }

    res.add("en") // prefer EN language over the standard OSM name

    return res
}

