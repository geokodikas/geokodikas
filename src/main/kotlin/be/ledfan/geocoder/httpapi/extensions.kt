package be.ledfan.geocoder.httpapi

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.response.respond

suspend fun ApplicationCall.respondError(s: String) {
    respond(HttpStatusCode.InternalServerError, mapOf("status" to "error", "msg" to s))
}



