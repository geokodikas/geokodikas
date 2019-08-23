package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.entity.OsmWay
import java.sql.ResultSet
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class ReverseQueryBuilderFactory {

    fun createBuilder(table: SearchTable, debug: Boolean = false): ReverseQueryBuilder {
        return when (table) {
            SearchTable.Node -> {
                NodeReverseQueryBuilder(debug)
            }
            SearchTable.Way -> {
                WayReverseQueryBuilder(debug)
            }
            SearchTable.Relation -> {
                TODO()
            }
            SearchTable.AddressMappings -> {
                TODO()
            }
        }
    }

    fun processResult(table: SearchTable, row: ResultSet,
                      nodes: MutableList<OsmNode>,
                      ways: MutableList<OsmWay>,
                      relations: MutableList<OsmRelation>) {

        when (table) {
            SearchTable.Node -> {
                val node = OsmNode.fillFromRow(row)
                node.dynamicProperties["distance"] = row.getDouble("metric_distance").roundToInt()
                node.dynamicProperties["name"] = "TemporyName"
                nodes.add(node)
            }
            SearchTable.Way -> {
                val way = OsmWay.fillFromRow(row)
                way.dynamicProperties["distance"] = row.getDouble("metric_distance").roundToInt()
                way.dynamicProperties["name"] = "TemporyName"
                ways.add(way)
            }
            SearchTable.Relation -> {
                val relation = OsmRelation.fillFromRow(row)
                relation.dynamicProperties["distance"] = row.getDouble("metric_distance").roundToInt()
                relation.dynamicProperties["name"] = "TemporyName"
                relations.add(relation)
            }
            SearchTable.AddressMappings -> {

            }
        }

    }

}