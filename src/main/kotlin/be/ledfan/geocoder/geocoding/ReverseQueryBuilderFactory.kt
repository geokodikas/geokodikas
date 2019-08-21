package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.entity.OsmWay
import java.sql.ResultSet

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
                      nodes: MutableList<Reverse.Result<OsmNode>>,
                      ways: MutableList<Reverse.Result<OsmWay>>,
                      relations: MutableList<Reverse.Result<OsmRelation>>) {

        when (table) {
            SearchTable.Node -> {
                nodes.add(Reverse.Result(OsmNode.fillFromRow(row), row.getDouble("distance"), "testname"))
            }
            SearchTable.Way -> {
                ways.add(Reverse.Result(OsmWay.fillFromRow(row), row.getDouble("distance"), "testname"))
            }
            SearchTable.Relation -> {
                relations.add(Reverse.Result(OsmRelation.fillFromRow(row), row.getDouble("distance"), "testname"))
            }
            SearchTable.AddressMappings -> {

            }
        }

    }

}