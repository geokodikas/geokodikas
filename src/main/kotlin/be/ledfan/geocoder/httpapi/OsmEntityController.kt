package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.mapper.OsmParentMapper
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Routing
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.kodein.di.Kodein
import org.kodein.di.generic.instance

@KtorExperimentalLocationsAPI
class OsmEntityController(override val kodein: Kodein) : KodeinController(kodein) {

    private val osmWayMapper: OsmWayMapper by instance()
    private val osmParentMapper: OsmParentMapper by instance()

    private suspend fun getWay(route: Routes.OsmEntity.Way, call: ApplicationCall) {
        if (!listOf("html", "json").contains(route.formatting)) {
            return call.respondError("Format must be either html or json")
        }
        val ids = route.id.split(",").map { it.toLong() }

        val entities = osmWayMapper.getByPrimaryIds(ids)

        val jsonResponseBuilder = JSONResponseBuilder()
        val htmlResponseBuilder = HTMLResponseBuilder()
        entities.forEach { jsonResponseBuilder.addEntity(it.value) }

        val parents = osmParentMapper.getParents(entities.values.toList())
        val geoJson = jsonResponseBuilder.buildAsCollection()

        if (route.formatting == "html") {
            val tabs = htmlResponseBuilder.buildTabs(
                    entities.mapValues { (_, entity) ->
                        createHTML().div {
                            ul {
                                classes = setOf("list-group")

                                li {
                                    classes = setOf("list-group-item", "list-group-item-primary")
                                    +"${entity.id}"
                                }

                                li {
                                    classes = setOf("list-group-item")
                                    +"Way"
                                }

                                li {
                                    classes = setOf("list-group-item")
                                    +"Layer is ${entity.layer}"
                                }

                                li {
                                    if (entity.hasOneWayRestriction) {
                                        classes = setOf("list-group-item", "list-group-item-success")
                                        +"Has one way restriction"
                                    } else {
                                        classes = setOf("list-group-item", "list-group-item-danger")
                                        +"No one way restriction"
                                    }
                                }
                            }

                            br()
                            unsafe { +htmlResponseBuilder.buildParentTable(parents[entity.id]) }
                            br()
                            unsafe { +htmlResponseBuilder.buildTagTable(entity.tags) }
                        }
                    })
            call.respond(FreeMarkerContent("map_html.ftl",
                    mapOf("geojson" to geoJson.toJsonString(true),
                            "tabs" to tabs), null))
        } else {
            call.respond(geoJson)
        }
    }

    override fun Routing.registerRoutes() {
        get<Routes.OsmEntity.Way> { route -> getWay(route, this.call) }
    }

}


