package be.ledfan.geocoder.db.entity

enum class OsmType(val tableName: String) {
    Node("osm_node"),
    Way("osm_way"),
    Relation("osm_relation"),
    AddressIndex("address_index")
}