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

    private fun <T : OsmEntity> get(mapper: Mapper<T>, route: Routes.OsmEntity.OsmEntityRoute): List<T> {

        if (!listOf("html", "json").contains(route.formatting)) {
            throw HttpApiBadRequestException("Format must be either html or json")
        }

        val ids = route.id.split(",").map { it.toLong() }
        val entities = mapper.getByPrimaryIdsAsList(ids)

        if (entities.isEmpty()) {
            throw HttpApiNotFoundException("No entities found for ids: '${route.id}'")
        }
        if (entities.size != ids.size) {
            throw HttpApiNotFoundException("No entities found for all ids, missing: '${ids.subtract(entities.map { it.id })}'")
        }

        return entities
    }

    private fun toGeoJson(entities: List<OsmEntity>): JsonObject {
        val jsonResponseBuilder = JSONResponseBuilder()
        entities.forEach { jsonResponseBuilder.addEntity(it) }
        return jsonResponseBuilder.toJson()

    }

    override fun Routing.registerRoutes() {
        get<Routes.OsmEntity.Way> { route ->
            withErrorHandling(call) {
                val entities = get(osmWayMapper, route)
                val geoJson = toGeoJson(entities)
                if (route.formatting == "html") {
                    call.respond(htmlViewer.createHtml(geoJson = geoJson, ways = entities, nodes = listOf(), relations = listOf(), addresses = listOf()))
                } else {
                    call.respond(geoJson)
                }
            }
        }
        get<Routes.OsmEntity.Node> { route ->
            withErrorHandling(call) {
                val entities = get(osmNodeMapper, route)
                val geoJson = toGeoJson(entities)
                if (route.formatting == "html") {
                    call.respond(htmlViewer.createHtml(geoJson = geoJson, ways = listOf(), nodes = entities, relations = listOf(), addresses = listOf()))
                } else {
                    call.respond(geoJson)
                }
            }
        }
        get<Routes.OsmEntity.Relation> { route ->
            withErrorHandling(call) {
                val entities = get(osmRelationMapper, route)
                val geoJson = toGeoJson(entities)
                if (route.formatting == "html") {
                    call.respond(htmlViewer.createHtml(geoJson = geoJson, ways = listOf(), nodes = listOf(), relations = entities, addresses = listOf()))
                } else {
                    call.respond(geoJson)
                }
            }
        }
        get<Routes.OsmEntity.Any> { route ->
            withErrorHandling(call) {
                val ids = route.id.split(",").map { it.toLong() }

                val nodes = osmNodeMapper.getByPrimaryIdsAsList(ids)
                val ways = osmWayMapper.getByPrimaryIdsAsList(ids)
                val relations = osmRelationMapper.getByPrimaryIdsAsList(ids)
                val geoJson = toGeoJson(nodes + ways + relations)

                if (route.formatting == "html") {
                    call.respond(htmlViewer.createHtml(geoJson = geoJson, ways = ways, nodes = nodes, relations = relations, addresses = listOf()))
                } else {
                    call.respond(geoJson)
                }
            }
        }
    }

}


