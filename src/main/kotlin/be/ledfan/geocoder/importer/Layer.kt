package be.ledfan.geocoder.importer

enum class Layer(val order: Int) {

    VirtualTrafficFlow(130),    // node
    PhysicalTrafficFlow(130),   // node
    Junction(120),              // node
    Link(110),                  // way
    Venue(100),                 // node
    Address(99),                // node
    Street(98),                 // way
    Neighbourhood(97),
    LocalAdmin(96),
    County(95),
    MacroRegion(94),
    Country(93),
    Superfluous(0),             // node, way -> Do Not Import!
}