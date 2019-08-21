package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.importer.Layer

enum class SearchTable(val rawTable: String) {
    Node("osm_node"),
    Way("osm_way"),
    Relation("osm_relation"),
    AddressMappings("")
}

/**
 * Maps a [Layer] to a list of tables in which this layer may appear.
 */
val layerMapping = mapOf(
        Layer.VirtualTrafficFlow to listOf(SearchTable.Node, SearchTable.Way),
        Layer.PhysicalTrafficFlow to listOf(SearchTable.Node),
        Layer.Junction to listOf(SearchTable.Node, SearchTable.Way),
        Layer.Link to listOf(SearchTable.Way),
//        Layer.Venue to listOf(SearchTable.AddressMappings),
//        Layer.Address to listOf(SearchTable.AddressMappings),
        Layer.Street to listOf(SearchTable.Way)
//        Layer.Neighbourhood to listOf(SearchTable.Relation),
//        Layer.LocalAdmin to listOf(SearchTable.Relation),
//        Layer.County to listOf(SearchTable.Relation),
//        Layer.MacroRegion to listOf(SearchTable.Relation),
//        Layer.Country to listOf(SearchTable.Relation)
)

fun getTablesForLayers(layers: List<Layer>): HashSet<SearchTable> {
    val tables = HashSet<SearchTable>()
    for (layer in layers) {
        layerMapping[layer]?.let {
            tables.addAll(it)
        }
        if (tables.size == SearchTable.values().size) {
            break // already need to search in tables, not necessary to continue
        }
    }

    return tables
}
