package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.mapper.*
import com.beust.klaxon.JsonObject
import io.ktor.application.ApplicationCall
import io.ktor.application.call
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
    private val addressIndexMapper: AddressIndexMapper by instance()
    private val htmlViewer = HTMLViewer(wayNodeMapper, osmParentMapper, addressIndexMapper)

    //    buildHtml: (JsonObject, List<T>) -> FreeMarkerContent
    private suspend fun <T : OsmEntity> get(mapper: Mapper<T>, route: Routes.OsmEntity.OsmEntityRoute, call: ApplicationCall): List<T>? {

        if (!listOf("html", "json").contains(route.formatting)) {
            call.respondError("Format must be either html or json")
            return null
        }

        val ids = route.id.split(",").map { it.toLong() }
        val entities = mapper.getByPrimaryIds(ids)

//        if (entities.size == 0) {
//            call.respondNotFound("""No entities found for ids: '${route.id}'""")
//            return null
//        }
//        if (entities.size != ids.size) {
//            call.respondNotFound("""No entities found for ids: '${ids.subtract(entities.keys.toList())}'""")
//            return null
//        }

        return entities.values.toList()
    }

    private fun toGeoJson(entities: List<OsmEntity>): JsonObject {
        val jsonResponseBuilder = JSONResponseBuilder()
        entities.forEach { jsonResponseBuilder.addEntity(it) }
        return jsonResponseBuilder.toJson()

    }

    override fun Routing.registerRoutes() {
        get<Routes.OsmEntity.Way> { route ->
            val entities = get(osmWayMapper, route, this.call) ?: return@get
            val geoJson = toGeoJson(entities)
            if (route.formatting == "html") {
                call.respond(htmlViewer.createHtml(geoJson = geoJson, ways = entities, nodes = listOf(), relations = listOf()))
            } else {
                call.respond(geoJson)
            }
        }
        get<Routes.OsmEntity.Node> { route ->
            val entities = get(osmNodeMapper, route, this.call) ?: return@get
            val geoJson = toGeoJson(entities)
            if (route.formatting == "html") {
                call.respond(htmlViewer.createHtml(geoJson = geoJson, ways = listOf(), nodes = entities, relations = listOf()))
            } else {
                call.respond(geoJson)
            }
        }
        get<Routes.OsmEntity.Relation> { route ->
            val entities = get(osmRelationMapper, route, this.call) ?: return@get
            val geoJson = toGeoJson(entities)
            if (route.formatting == "html") {
                call.respond(htmlViewer.createHtml(geoJson = geoJson, ways = listOf(), nodes = listOf(), relations = entities))
            } else {
                call.respond(geoJson)
            }
        }
        get<Routes.OsmEntity.Any> { route ->
            val nodes = get(osmNodeMapper, route, this.call) ?: return@get
            val ways = get(osmWayMapper, route, this.call) ?: return@get
            val relations = get(osmRelationMapper, route, this.call) ?: return@get
            val geoJson = toGeoJson(nodes + ways + relations)
            if (route.formatting == "html") {
                call.respond(htmlViewer.createHtml(geoJson = geoJson, ways = ways, nodes = nodes, relations = relations))
            } else {
                call.respond(geoJson)
            }
        }
    }

}


