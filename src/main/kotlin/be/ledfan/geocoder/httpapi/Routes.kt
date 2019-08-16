package be.ledfan.geocoder.httpapi

import io.ktor.locations.Location

object Routes {

    @Location("/")
    object Root : TypedRoute

    @Location("/api/v1/reverse")
    object Reverse : TypedRoute

    val definedRoutes = arrayListOf<TypedRoute>().also {
        it.add(Root)
        it.add(Reverse)
    }

}