package be.ledfan.geocoder.httpapi

import io.ktor.locations.Location

object Routes {

    @Location("/")
    object Root : TypedRoute

    @Location("/api/v1/reverse")
    object Reverse : TypedRoute

    object OsmEntity {
        open class OsmEntityRoute(open val id: String, open val formatting: String = "json") : TypedRoute

        @Location("/api/v1/osm_entity/way/{id}")
        data class Way(override val id: String, override val formatting: String = "json") : OsmEntityRoute(id, formatting)

        @Location("/api/v1/osm_entity/relation/{id}")
        data class Relation(override val id: String, override val formatting: String = "json") : OsmEntityRoute(id, formatting)

        @Location("/api/v1/osm_entity/node/{id}")
        data class Node(override val id: String, override val formatting: String = "json") : OsmEntityRoute(id, formatting)

    }

    val exampleRoutes = arrayListOf<TypedRoute>().also {
        it.add(Root)
        it.add(Reverse)
        it.add(OsmEntity.Way(id = "8061263", formatting = "json"))
        it.add(OsmEntity.Way(id = "8061263", formatting = "html"))
        it.add(OsmEntity.Way(id = "8061263,8061267", formatting = "json"))
        it.add(OsmEntity.Way(id = "8061263,8061267", formatting = "html"))
        it.add(OsmEntity.Node(id = "1422600738", formatting = "json"))
        it.add(OsmEntity.Node(id = "1422600738", formatting = "html"))
        it.add(OsmEntity.Node(id = "1422600738,1889049559", formatting = "json"))
        it.add(OsmEntity.Node(id = "1422600738,1889049559", formatting = "html"))
        it.add(OsmEntity.Relation(id = "52411", formatting = "json"))
        it.add(OsmEntity.Relation(id = "52411", formatting = "html"))
        it.add(OsmEntity.Relation(id = "53134,90348", formatting = "json"))
        it.add(OsmEntity.Relation(id = "53134,90348", formatting = "html"))
    }

}