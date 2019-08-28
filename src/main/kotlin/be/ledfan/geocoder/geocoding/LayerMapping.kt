package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.entity.OsmType
import be.ledfan.geocoder.importer.Layer

/**
 * Maps a [Layer] to a list of tables in which this layer may appear.
 */
val layerMapping = mapOf(
        Layer.VirtualTrafficFlow to listOf(OsmType.Node, OsmType.Way),
        Layer.PhysicalTrafficFlow to listOf(OsmType.Node),
        Layer.Junction to listOf(OsmType.Node, OsmType.Way),
        Layer.Link to listOf(OsmType.Way),
        Layer.Venue to listOf(OsmType.AddressIndex),
        Layer.Address to listOf(OsmType.AddressIndex),
        Layer.Street to listOf(OsmType.Way),
        Layer.Neighbourhood to listOf(OsmType.Relation),
        Layer.LocalAdmin to listOf(OsmType.Relation),
        Layer.County to listOf(OsmType.Relation),
        Layer.MacroRegion to listOf(OsmType.Relation),
        Layer.Country to listOf(OsmType.Relation)
)

fun getTablesForLayers(layers: List<Layer>): HashSet<OsmType> {
    val tables = HashSet<OsmType>()
    for (layer in layers) {
        layerMapping[layer]?.let {
            tables.addAll(it)
        }
        if (tables.size == OsmType.values().size) {
            break // already need to search in tables, not necessary to continue
        }
    }

    return tables
}
