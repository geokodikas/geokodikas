package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.mapper.*
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
    private val osmRelationMapper: OsmRelationMapper by instance()
    private val wayNodeMapper: WayNodeMapper by instance()

    private suspend fun <T : OsmEntity> get(mapper: Mapper<T>, route: Routes.OsmEntity.OsmEntityRoute, call: ApplicationCall,
                                            buildHtml: (HashMap<Long, T>, HashMap<Long, ArrayList<OsmRelation>>) -> String) {

        if (!listOf("html", "json").contains(route.formatting)) {
            return call.respondError("Format must be either html or json")
        }
        val ids = route.id.split(",").map { it.toLong() }
        val jsonResponseBuilder = JSONResponseBuilder()
        val entities = mapper.getByPrimaryIds(ids)
        entities.forEach { jsonResponseBuilder.addEntity(it.value) }

        val parents = osmParentMapper.getParents(entities.values.toList())
        val geoJson = jsonResponseBuilder.buildAsCollection()

        if (route.formatting == "html") {
            val tabs = buildHtml(entities, parents)
            call.respond(FreeMarkerContent("map_html.ftl",
                    mapOf("geojson" to geoJson.toJsonString(true),
                            "tabs" to tabs), null))
        } else {
            call.respond(geoJson)
        }
    }

    override fun Routing.registerRoutes() {
        get<Routes.OsmEntity.Way> { route ->
            val htmlResponseBuilder = HTMLResponseBuilder()
            get(osmWayMapper, route, this.call) { entities, parents ->
                val nodes = wayNodeMapper.getLinkedNodesByWay(entities.values)
                htmlResponseBuilder.buildWay(entities, parents, nodes)
            }
        }
        get<Routes.OsmEntity.Node> { route ->
            val htmlResponseBuilder = HTMLResponseBuilder()
            get(osmNodeMapper, route, this.call, htmlResponseBuilder::buildNode)
        }
        get<Routes.OsmEntity.Relation> { route ->
            val htmlResponseBuilder = HTMLResponseBuilder()
            get(osmRelationMapper, route, this.call, htmlResponseBuilder::buildRelation)
        }
    }

}


