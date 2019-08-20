package be.ledfan.geocoder.httpapi

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.response.respond

suspend fun ApplicationCall.respondError(s: String) {
    respond(mapOf("status" to "error", "msg" to s))
}

suspend fun ApplicationCall.respondNotFound(s: String) {
    respond(NotFound, mapOf("status" to "error", "msg" to s))
}

