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

    val definedRoutes = arrayListOf<TypedRoute>().also {
        it.add(Root)
        it.add(Reverse)
        it.add(OsmEntity.Way("0"))
    }

}