package be.ledfan.geocoder.httpapi

import io.ktor.application.ApplicationCall
import io.ktor.response.respond

suspend fun ApplicationCall.respondError(s: String) {
    respond(mapOf("status" to "error", "msg" to s))
}

