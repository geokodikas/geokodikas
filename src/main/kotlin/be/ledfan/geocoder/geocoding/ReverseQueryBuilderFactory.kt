package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.entity.*
import java.sql.ResultSet
import kotlin.math.roundToInt

class ReverseQueryBuilderFactory {

    fun createBuilder(table: OsmType, debug: Boolean = false): ReverseQueryBuilder {
        return when (table) {
            OsmType.Node -> {
                NodeReverseQueryBuilder(debug)
            }
            OsmType.Way -> {
                WayReverseQueryBuilder(debug)
            }
            OsmType.Relation -> {
                TODO()
            }
            OsmType.AddressIndex -> {
                AddressIndexReverseQueryBuilder(debug)
            }
        }
    }

    fun processResult(table: OsmType, row: ResultSet,
                      nodes: MutableList<OsmNode>,
                      ways: MutableList<OsmWay>,
                      relations: MutableList<OsmRelation>,
                      addresses: MutableList<AddressIndex>) {

        when (table) {
            OsmType.Node -> {
                val node = OsmNode.fillFromRow(row)
                node.dynamicProperties["distance"] = row.getDouble("metric_distance").roundToInt()
                node.dynamicProperties["name"] = "TemporyName"
                nodes.add(node)
            }
            OsmType.Way -> {
                val way = OsmWay.fillFromRow(row)
                way.dynamicProperties["distance"] = row.getDouble("metric_distance").roundToInt()
                way.dynamicProperties["name"] = "TemporyName"
                ways.add(way)
            }
            OsmType.Relation -> {
                val relation = OsmRelation.fillFromRow(row)
                relation.dynamicProperties["distance"] = row.getDouble("metric_distance").roundToInt()
                relation.dynamicProperties["name"] = "TemporyName"
                relations.add(relation)
            }
            OsmType.AddressIndex -> {
                val addressIndex = AddressIndex.fillFromRow(row)
                // TODO
                addresses.add(addressIndex)
            }
        }

    }

}