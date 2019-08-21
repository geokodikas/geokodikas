package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.importer.Layer


class Reverse(private val con: ConnectionWrapper) {

    data class Result<T: OsmEntity>(val osmWay: T, val distance: Double, val name: String)

    private val reverseQueryBuilderFactory = ReverseQueryBuilderFactory()

    fun reverseGeocode(lat: Double, lon: Double,
                       limitNumeric: Int?, limitRadius: Int?,
                       desiredLayers: List<String>?):
            Triple<ArrayList<Result<OsmNode>>, ArrayList<Result<OsmWay>>, ArrayList<Result<OsmRelation>>> {

        val layers = if (desiredLayers == null || desiredLayers.isEmpty())  {
            Layer.values().toList()
        } else {
            desiredLayers.map { Layer.valueOf(it) }
        }

        val requiredTables = getTablesForLayers(layers)

        val nodes = ArrayList<Result<OsmNode>>()
        val ways = ArrayList<Result<OsmWay>>()
        val relations = ArrayList<Result<OsmRelation>>()

        for (table in requiredTables) {
            val (sqlQuery, parameters) = reverseQueryBuilderFactory.createBuilder(table, debug = true).run {
                baseQuery(lat, lon, requiredTables)
                if (limitRadius != null && limitRadius > 0) {
                    whereMetricDistance(limitRadius)
                }
                if (desiredLayers != null) {
                    whereLayer(layers)
                }
                orderBy()
                if (limitNumeric != null && limitNumeric > 0) {
                    limit(limitNumeric)
                } else {
                    limit(5)
                }
                build()
            }

            val stmt = con.prepareStatement(sqlQuery)

            parameters.forEachIndexed { index, param ->
                stmt.setObject(index + 1, param)
            }

            val result = stmt.executeQuery()
            while (result.next()) {
                reverseQueryBuilderFactory.processResult(table, result, nodes, ways, relations)
            }

            stmt.close()
            result.close()
        }

        return Triple(nodes, ways, relations)
    }

}