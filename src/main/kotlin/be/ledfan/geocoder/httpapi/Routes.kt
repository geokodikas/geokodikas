package be.ledfan.geocoder.httpapi

import io.ktor.locations.Location

object Routes {

    @Location("/")
    object Root : TypedRoute

    @Location("/api/v1/reverse")
    object Reverse : TypedRoute

    object OsmEntity {
        @Location("/api/v1/osm_entity/way/{id}")
        data class Way(val id: Long, val formatting: String = "json") : TypedRoute

        @Location("/api/v1/osm_entity/relation/{id}")
        data class Relation(val id: Long, val formatting: String = "json") : TypedRoute
    }

    val definedRoutes = arrayListOf<TypedRoute>().also {
        it.add(Root)
        it.add(Reverse)
        it.add(OsmEntity.Way(0))
    }

}