package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.OsmRelation
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

        val parsedTags = TagParser().parse(tags) // TODO DI
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


}