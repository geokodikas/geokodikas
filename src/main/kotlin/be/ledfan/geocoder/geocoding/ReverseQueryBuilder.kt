package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.entity.OsmType
import be.ledfan.geocoder.forFirstAndRest
import be.ledfan.geocoder.importer.Layer

abstract class ReverseQueryBuilder(private val debug: Boolean = false) {

    private var hasWhere = false

    protected var currentQuery = ""
    protected val parameters = ArrayList<Any>()

    abstract fun specificBaseQuery(lon: Double, lat: Double, metricDistance: Int)

    fun baseQuery(lat: Double, lon: Double, metricDistance: Int, searchTables: HashSet<OsmType>): ReverseQueryBuilder {
        if (searchTables.isEmpty()) throw Exception("Need to search in at least one table")
        specificBaseQuery(lon, lat, metricDistance)
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
            println("Filled in query\n: $filledQuery")
        }
        return Pair(currentQuery, parameters)
    }

    protected fun withWhere(where: String) {
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