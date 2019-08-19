package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.mapper.OsmParentMapper
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.importer.core.Tags
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.locations.locations
import io.ktor.response.respond
import io.ktor.routing.Routing
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.instance
import kotlinx.html.*
import kotlinx.html.stream.createHTML

@KtorExperimentalLocationsAPI
class OsmEntityController(override val kodein: Kodein) : KodeinController(kodein) {

    private suspend fun getWay(route: Routes.OsmEntity.Way, call: ApplicationCall) {
        if (!listOf("html", "json").contains(route.formatting)) {
            return call.respondError("Format must be either html or json")
        }
        if (route.formatting == "html") {
            // only allow one id
        }

        // get some objects from DB, TODO proper DI
        val osmWayMapper = kodein.direct.instance<OsmWayMapper>()
        val osmParentMapper = kodein.direct.instance<OsmParentMapper>()

        val entities = osmWayMapper.getByPrimaryIds(listOf(route.id))

        val responseBuilder = ResponseBuilder()
        entities.forEach { responseBuilder.addEntity(it.value) }

        val parents = osmParentMapper.getParents(entities.values.toList())

        val geoJson = responseBuilder.buildAsSingleFeautre()
        if (route.formatting == "html") {
            val entity = entities.values.first()
            val parsedTags = TagParser().parse(entity.tags) // TODO DI

            fun recurse(children: HashMap<String, Tags>): String {
                return createHTML().table {
                    classes = setOf("table", "table-striped", "table-sm")
                    tr {
                        th {
                            +"Key"
                        }
                        th {
                            +"Value"
                        }
                    }
                    children.forEach { (key, children) ->
                        tr {
                            td {
                                +key
                            }
                            td {
                                if (children.amountOfChildren > 1) {
                                    ul {
                                        children.values?.forEach { value ->
                                            li {
                                                +value
                                            }
                                        }
                                    }
                                } else {
                                    children.values?.firstOrNull()?.let { value ->
                                        +value
                                    }
                                }
                                if (children.amountOfChildren > 0) {
                                    unsafe {
                                        +recurse(children.children)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val parentsOfEntity = parents[entity.id]

            val parentHtml =
                    if (parentsOfEntity != null) {
                        createHTML().table {
                            classes = setOf("table", "table-striped", "table-sm")
                            parentsOfEntity.forEach { parent ->
                                tr {
                                    td {
                                        text(parent.layer.toString())
                                    }
                                    td {
                                        parent.name?.let {
                                            a(application.locations.href(Routes.OsmEntity.Relation(parent.id, "html"))) {
                                                text(it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        ""
                    }


            val tagsAsHtml = recurse(parsedTags.children).toString()


            call.respond(FreeMarkerContent("map_html.ftl",
                    mapOf("geojson" to geoJson.toJsonString(true),
                            "tags_html" to tagsAsHtml,
                            "osm_id" to entity.id,
                            "osm_type" to "way",
                            "layer" to entity.layer.toString(),
                            "has_one_way_restriction" to entity.hasOneWayRestriction,
                            "has_reversed_one_way" to entity.hasReversedOneWay,
                            "parent_html" to parentHtml,
                            "as_json_link" to application.locations.href(Routes.OsmEntity.Way(route.id, "json"))
                    ), null))
        } else {
            call.respond(geoJson)
        }
    }

    override fun Routing.registerRoutes() {
        get<Routes.OsmEntity.Way> { route -> getWay(route, this.call) }
    }

}


