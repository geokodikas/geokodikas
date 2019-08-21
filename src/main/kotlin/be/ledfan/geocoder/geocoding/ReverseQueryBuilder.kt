package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.forFirstAndRest
import be.ledfan.geocoder.importer.Layer

abstract class ReverseQueryBuilder(private val debug: Boolean = false) {

    private var hasWhere = false

    private var currentQuery = ""
    private val parameters = ArrayList<Any>()

    abstract fun cteQuery(lon: Double, lat: Double): String

    fun baseQuery(lat: Double, lon: Double, searchTables: HashSet<SearchTable>): ReverseQueryBuilder {
        if (searchTables.isEmpty()) throw Exception("Need to search in at least one table")
        currentQuery = "WITH cte_query AS ("
        currentQuery += cteQuery(lon, lat)
        currentQuery += ") SELECT * FROM cte_query "
        return this
    }

    fun orderBy(): ReverseQueryBuilder {
        val sql = """
            ORDER BY metric_distance
            """
        currentQuery += sql
        return this
    }

    fun build(): Pair<String, List<Any>> {
        if (debug) {
            var filledQuery = currentQuery
            parameters.forEach { parameterValue ->
                filledQuery = filledQuery.replaceFirst("?", parameterValue.toString())
            }
            println("Filledquery: $filledQuery")
        }
        return Pair(currentQuery, parameters)
    }

    private fun withWhere(where: String) {
        currentQuery += if (!hasWhere) {
            hasWhere = true
            """
            WHERE $where
            """
        } else {
            """
            AND $where
            """
        }
    }

    fun whereMetricDistance(metricDistance: Int): ReverseQueryBuilder {
        withWhere("metric_distance < ?")
        parameters.add(metricDistance)
        return this
    }

    fun limit(limit: Int): ReverseQueryBuilder {
        currentQuery += "LIMIT ?"
        parameters.add(limit)
        return this
    }

    fun whereLayer(layers: List<Layer>): ReverseQueryBuilder {
        if (layers.isEmpty()) return this

        var whereClause = "("
        layers.forFirstAndRest({ layer ->
            //            if (!isValidLayer(layer)) throw Exception("Invalid layer $layer")
            whereClause += "layer = ?::Layer "
            parameters.add(layer.toString())
        }, { layer ->
            //            if (!isValidLayer(layer)) throw Exception("Invalid layer $layer")
            whereClause += "OR layer = ?::Layer "
            parameters.add(layer.toString())
        })
        whereClause += ")"

        withWhere(whereClause)
        return this
    }

}