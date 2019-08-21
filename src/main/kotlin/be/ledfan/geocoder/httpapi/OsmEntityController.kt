package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.mapper.*
import com.beust.klaxon.JsonObject
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Routing
import org.kodein.di.Kodein
import org.kodein.di.generic.instance

@KtorExperimentalLocationsAPI
class OsmEntityController(override val kodein: Kodein) : KodeinController(kodein) {

    private val osmNodeMapper: OsmNodeMapper by instance()
    private val osmWayMapper: OsmWayMapper by instance()
    private val osmParentMapper: OsmParentMapper by instance()
    private val wayNodeMapper: WayNodeMapper by instance()
    private val osmRelationMapper: OsmRelationMapper by instance()
    private val htmlViewer = HTMLViewer(wayNodeMapper, osmParentMapper)

    private suspend fun <T : OsmEntity> get(mapper: Mapper<T>, route: Routes.OsmEntity.OsmEntityRoute, call: ApplicationCall,
                                            buildHtml: (JsonObject, List<T>) -> FreeMarkerContent) {

        if (!listOf("html", "json").contains(route.formatting)) {
            return call.respondError("Format must be either html or json")
        }

        val ids = route.id.split(",").map { it.toLong() }

        val jsonResponseBuilder = JSONResponseBuilder()
        val entities = mapper.getByPrimaryIds(ids)

        if (entities.size == 0) {
            return call.respondNotFound("""No entities found for ids: '${route.id}'""")
        }
        if (entities.size != ids.size) {
            return call.respondNotFound("""No entities found for ids: '${ids.subtract(entities.keys.toList())}'""")
        }

        entities.forEach { jsonResponseBuilder.addEntity(it.value) }

        val geoJson = jsonResponseBuilder.toJson()

        if (route.formatting == "html") {
            call.respond(buildHtml(geoJson, entities.values.toList()))
        } else {
            call.respond(geoJson)
        }
    }

    override fun Routing.registerRoutes() {
        get<Routes.OsmEntity.Way> { route ->
            get(osmWayMapper, route, this.call) { geoJson, ways ->
                htmlViewer.createHtml(geoJson = geoJson, ways = ways, nodes = listOf(), relations = listOf())
            }
        }
        get<Routes.OsmEntity.Node> { route ->
            get(osmNodeMapper, route, this.call) { geoJson, nodes ->
                htmlViewer.createHtml(geoJson = geoJson, ways = listOf(), nodes = nodes, relations = listOf())
            }
        }
        get<Routes.OsmEntity.Relation> { route ->
            get(osmRelationMapper, route, this.call) { geoJson, relations ->
                htmlViewer.createHtml(geoJson = geoJson, ways = listOf(), nodes = listOf(), relations = relations)
            }
        }
    }

}


