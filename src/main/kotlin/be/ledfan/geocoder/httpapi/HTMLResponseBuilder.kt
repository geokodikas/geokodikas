package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.importer.core.Tags
import io.ktor.application.Application
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import be.ledfan.geocoder.kodein
import io.ktor.locations.locations
import org.kodein.di.direct
import org.kodein.di.generic.instance

class HTMLResponseBuilder {

    private val application = kodein.direct.instance<Application>()

    fun buildTagTable(tags: Map<String, String>): String {
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

        val parsedTags = TagParser().parse(tags)
        return recurse(parsedTags.children)
    }

    fun buildParentTable(parentsOfEntity: java.util.ArrayList<OsmRelation>?): String {
        return if (parentsOfEntity != null) {
            createHTML().table {
                classes = setOf("table", "table-striped", "table-sm")
                parentsOfEntity.forEach { parent ->
                    tr {
                        td {
                            text(parent.layer.toString())
                        }
                        td {
                            parent.name?.let {
                                a(application.locations.href(Routes.OsmEntity.Relation(parent.id.toString(), "html"))) {
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
    }

    fun buildTabs(tabs: Map<Long, String>): String {
        return createHTML().div {
            ul {
                classes = setOf("nav", "nav-tabs")
                id = "myTab"
                attributes["role"] = "tablist"

                var first = true
                tabs.forEach { (tabId, tabHtml) ->
                    li {
                        classes = setOf("nav-item")
                        a {
                            if (first) {
                                classes = setOf("nav-link", "active")
                                first = false
                            } else {
                                classes = setOf("nav-link")
                            }
                            id = "tab-btn-$tabId"
                            attributes["data-toggle"] = "tab"
                            attributes["href"] = "#tab-$tabId"
                            attributes["role"] = "tab"

                            text(tabId)
                        }
                    }
                }
            }

            div {
                classes = setOf("tab-content")
                id = "myTabContent"
                var first = true
                tabs.forEach { (tabId, tabHtml) ->
                    div {
                        if (first) {
                            classes = setOf("tab-pane", "fade", "show", "active")
                            first = false
                        } else {
                            classes = setOf("tab-pane", "fade")
                        }
                        id = "tab-$tabId"
                        attributes["role"] = "tabpanel"
                        unsafe { +tabHtml }
                    }
                }

            }
        }
    }

    fun buildWay(entities: List<OsmWay>, parents: HashMap<Long, ArrayList<OsmRelation>>, nodes: Map<Long, List<OsmNode>>): HashMap<Long, String> {
        return HashMap(entities.associateBy { it.id }.mapValues { (_, entity) ->
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
                unsafe { +buildNodesTable(nodes[entity.id]) }

                br()
                unsafe { +buildParentTable(parents[entity.id]) }

                br()
                unsafe { +buildTagTable(entity.tags) }
            }
        })
    }

    private fun buildNodesTable(relatedNodes: List<OsmNode>?): String {
        return if (relatedNodes != null) {
            createHTML().table {
                classes = setOf("table", "table-striped", "table-sm")
                relatedNodes.forEach { node ->
                    tr {
                        td {
                            text(node.layer.toString())
                        }
                        td {
                            a(application.locations.href(Routes.OsmEntity.Node(node.id.toString(), "html"))) {
                                text(node.id)
                            }
                        }
                    }
                }
            }
        } else {
            ""
        }
    }

    private fun buildWaysTable(relatedWays: List<OsmWay>?): String {
        return if (relatedWays != null) {
            createHTML().table {
                classes = setOf("table", "table-striped", "table-sm")
                relatedWays.forEach { node ->
                    tr {
                        td {
                            text(node.layer.toString())
                        }
                        td {
                            a(application.locations.href(Routes.OsmEntity.Way(node.id.toString(), "html"))) {
                                text(node.id)
                            }
                        }
                    }
                }
            }
        } else {
            ""
        }
    }

    fun buildNode(entities: List<OsmNode>, parents: HashMap<Long, ArrayList<OsmRelation>>, ways: HashMap<Long, ArrayList<OsmWay>>): HashMap<Long, String> {
        return HashMap(entities.associateBy { it.id }.mapValues { (_, entity) ->
            createHTML().div {
                ul {
                    classes = setOf("list-group")

                    li {
                        classes = setOf("list-group-item", "list-group-item-primary")
                        +"${entity.id}"
                    }

                    li {
                        classes = setOf("list-group-item")
                        +"Node"
                    }

                    li {
                        classes = setOf("list-group-item")
                        +"Layer is ${entity.layer}"
                    }
                }

                br()
                unsafe { +buildWaysTable(ways[entity.id]) }

                br()
                unsafe { +buildParentTable(parents[entity.id]) }
                br()
                unsafe { +buildTagTable(entity.tags) }
            }
        })
    }

    fun buildRelation(entities: List<OsmRelation>, parents: HashMap<Long, ArrayList<OsmRelation>>): HashMap<Long, String> {
        return HashMap(entities.associateBy { it.id }.mapValues { (_, entity) ->
            createHTML().div {
                ul {
                    classes = setOf("list-group")

                    li {
                        classes = setOf("list-group-item", "list-group-item-primary")
                        +"${entity.id}"
                    }

                    li {
                        classes = setOf("list-group-item", "list-group-item-primary")
                        +"${entity.name}"
                    }

                    li {
                        classes = setOf("list-group-item")
                        +"Relation"
                    }

                    li {
                        classes = setOf("list-group-item")
                        +"Layer is ${entity.layer}"
                    }
                }

                br()
                unsafe { +buildParentTable(parents[entity.id]) }
                br()
                unsafe { +buildTagTable(entity.tags) }
            }
        })

    }

}