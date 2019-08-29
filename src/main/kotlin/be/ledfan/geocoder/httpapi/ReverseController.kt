package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.mapper.AddressIndexMapper
import be.ledfan.geocoder.db.mapper.OsmParentMapper
import be.ledfan.geocoder.db.mapper.WayNodeMapper
import be.ledfan.geocoder.geo.Coordinate
import be.ledfan.geocoder.geo.toGeoJsonCoordinate
import be.ledfan.geocoder.geocoding.ReverseGeocoderService
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Routing
import org.kodein.di.Kodein
import org.kodein.di.generic.instance

@KtorExperimentalLocationsAPI
class ReverseController(override val kodein: Kodein) : KodeinController(kodein) {

    val con: ConnectionWrapper by instance()

    private val reverseGeocoder: ReverseGeocoderService by instance()
    private val osmParentMapper: OsmParentMapper by instance()
    private val wayNodeMapper: WayNodeMapper by instance()
    private val addressIndexMapper: AddressIndexMapper by instance()
    private val htmlViewer = HTMLViewer(wayNodeMapper, osmParentMapper, addressIndexMapper)


    private suspend fun reverse(route: Routes.Reverse, call: ApplicationCall) {
        val limitNumeric: Int? = call.request.queryParameters["limitNumeric"]?.toInt()
        val limitRadius: Int? = call.request.queryParameters["limitRadius"]?.toInt()
        val limitLayers: List<String>? = call.request.queryParameters["limitLayers"]?.split(",")?.filter { it.trim() != "" }

        val (closestPoint, order, nodes, ways, relations, addresses) = try {
            reverseGeocoder.reverseGeocode(
                    route.lat,
                    route.lon,
                    limitNumeric,
                    limitRadius,
                    limitLayers
            )
        } catch (e: Exception) {
            val msg = e.message
            e.printStackTrace()
            if (msg != null) {
                return call.respondError(msg)
            }
            return call.respondError("Unknown error occurred")
        }

        val jsonResponseBuilder = JSONResponseBuilder()
        jsonResponseBuilder.addFeature {
            withId("input-point")
            withGeometry {
                point(Coordinate(route.lon, route.lat))
            }
        }
        jsonResponseBuilder.addFeature {
            withId("closest-point")
            withGeometry {
                point(closestPoint.toGeoJsonCoordinate())
            }
        }
        (nodes + ways + relations + addresses).forEach {
            jsonResponseBuilder.addEntity(it)
        }

        val geoJson = jsonResponseBuilder.toJson()
        if (route.formatting == "html") {
            call.respond(htmlViewer.createHtml(geoJson, nodes, ways, relations, addresses, order))
        } else {
            call.respond(geoJson)
        }
    }

    override fun Routing.registerRoutes() {
        get<Routes.Reverse> { route -> reverse(route, this.call) }
    }

}