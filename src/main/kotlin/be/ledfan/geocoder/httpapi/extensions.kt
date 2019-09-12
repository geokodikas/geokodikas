package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.OsmType
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.URLBuilder
import io.ktor.locations.Locations
import io.ktor.locations.locations
import io.ktor.response.respond

suspend fun ApplicationCall.respondError(s: String) {
    respond(HttpStatusCode.InternalServerError, mapOf("status" to "error", "msg" to s))
}

fun Application.hrefToOsm(osmType: OsmType, osmId: Long, formatting: String): String {
    val typedRoute = when(osmType) {
        OsmType.Node -> Routes.OsmEntity.Node(osmId.toString())
        OsmType.Way -> Routes.OsmEntity.Way(osmId.toString())
        OsmType.Relation -> Routes.OsmEntity.Relation(osmId.toString())
        OsmType.AddressIndex -> Routes.Address(osmId.toString())
    }

    return locations.href(typedRoute, listOf(Pair("formatting", formatting)))
}

fun Application.hrefToAny(ids: List<Long>, formatting: String): String {
    return locations.href(Routes.OsmEntity.Any(ids.joinToString(",")), listOf(Pair("formatting", formatting)))
}

