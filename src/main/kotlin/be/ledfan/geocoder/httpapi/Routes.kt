package be.ledfan.geocoder.httpapi

import io.ktor.locations.Location

object Routes {

    @Location("/")
    object Root : TypedRoute

    @Location("/api/v1/reverse")
    data class Reverse(val lat: Double, val lon: Double) : TypedRoute

    object OsmEntity {
        open class OsmEntityRoute(open val id: String) : TypedRoute

        @Location("/api/v1/osm_entity/way/{id}")
        data class Way(override val id: String) : OsmEntityRoute(id)

        @Location("/api/v1/osm_entity/relation/{id}")
        data class Relation(override val id: String) : OsmEntityRoute(id)

        @Location("/api/v1/osm_entity/node/{id}")
        data class Node(override val id: String) : OsmEntityRoute(id)

        @Location("/api/v1/osm_entity/any/{id}")
        data class Any(override val id: String) : OsmEntityRoute(id)
    }

    @Location("/api/v1/address/{id}")
    data class Address(val id: String) : TypedRoute

    val exampleRoutes = arrayListOf<TypedRoute>().also {
        it.add(Address(id = "374685279"))
        it.add(OsmEntity.Way(id = "8061263"))
        it.add(OsmEntity.Way(id = "8061263,8061267"))
        it.add(OsmEntity.Node(id = "1422600738"))
        it.add(OsmEntity.Node(id = "1422600738,1889049559"))
        it.add(OsmEntity.Relation(id = "52411"))
        it.add(OsmEntity.Relation(id = "53134,90348"))
        it.add(Reverse(lat=51.332267409713985, lon=4.521034955978394))
    }

}