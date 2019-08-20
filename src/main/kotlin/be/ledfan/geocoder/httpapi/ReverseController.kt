package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.mapper.OsmParentMapper
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import be.ledfan.geocoder.db.mapper.WayNodeMapper
import be.ledfan.geocoder.geocoding.Reverse
import de.topobyte.osm4j.core.model.iface.OsmWay
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.lang.Exception

@KtorExperimentalLocationsAPI
class ReverseController(override val kodein: Kodein) : KodeinController(kodein) {

    val con : ConnectionWrapper by instance()

    private val reverseGeocoder = Reverse(con)
    private val osmWayMapper: OsmWayMapper by instance()
    private val osmParentMapper: OsmParentMapper by instance()
    private val wayNodeMapper: WayNodeMapper by instance()
    private val htmlViewer = HTMLViewer(wayNodeMapper, osmParentMapper)

    private suspend fun reverse(route: Routes.Reverse, call: ApplicationCall) {
        val limitNumeric: Int? = call.request.queryParameters["limitNumeric"]?.toInt()
        val limitRadius: Int? = call.request.queryParameters["limitRadius"]?.toInt()
        val limitLayers: List<String>? = call.request.queryParameters["limitLayers"]?.split(",")?.filter { it.trim() != "" }

        val results = try {
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
        results.forEach {
            jsonResponseBuilder.addEntity(it.osmWay) {
                withProperty("distance", it.distance)
                withProperty("name", it.name)
            }
        }


        val geoJson = jsonResponseBuilder.toJson()
        if (route.formatting == "html") {
            call.respond(htmlViewer.createHtml(geoJson, listOf(), results.map { it.osmWay }, listOf()))
        } else {
            call.respond(geoJson)
        }
    }

    override fun Routing.registerRoutes() {
        get<Routes.Reverse> { route -> reverse(route, this.call) }
        println("heiro")
    }

}