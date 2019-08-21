package be.ledfan.geocoder.importer

import java.lang.IllegalArgumentException

enum class Layer(val order: Int) {

    VirtualTrafficFlow(130),
    PhysicalTrafficFlow(130),
    Junction(120),
    Link(110),
    Venue(100),
    Address(99),
    Street(98),
    Neighbourhood(97),
    LocalAdmin(96),
    County(95),
    MacroRegion(94),
    Country(93),
    Superfluous(0),             // Do Not Import!
}

fun isValidLayer(layer: String): Boolean {
    return try {
        Layer.valueOf(layer)
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}