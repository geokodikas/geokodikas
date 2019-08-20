package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.forFirstAndRest
import be.ledfan.geocoder.importer.isValidLayer
import org.intellij.lang.annotations.Language

class ReverseQueryBuilder {

//    private var logger = KotlinLogging.logger {}

    private var hasWhere = false

    private var currentQuery = ""
    private val parameters = ArrayList<Any>()

    fun baseQuery(lat: Double, lon: Double): ReverseQueryBuilder {
        @Language("SQL")
        currentQuery = """
            WITH search AS (
                SELECT *,
                   ST_distance(ST_SetSRID(ST_Point(?, ?), 4326), geometry)                  AS distance,
                   st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326), geometry)       AS metric_distance,
                   st_asbinary(st_closestpoint(geometry, ST_SetSRID(ST_Point(?, ?), 4326))) AS closest_point
                FROM osm_way
                WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326), geometry, 0.006)
            )
            SELECT * FROM search
            """
        repeat(4) {
            parameters.add(lon)
            parameters.add(lat)
        }
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

    fun whereLayer(layers: List<String>): ReverseQueryBuilder {
        if (layers.isEmpty()) return this

        var whereClause = "("
        layers.forFirstAndRest({ layer ->
            if (!isValidLayer(layer)) throw Exception("Invalid layer $layer")
            whereClause += "layer = ?::Layer "
            parameters.add(layer)
        }, { layer ->
            if (!isValidLayer(layer)) throw Exception("Invalid layer $layer")
            whereClause += "OR layer = ?::Layer "
            parameters.add(layer)
        })
        whereClause += ")"

        withWhere(whereClause)
        return this
    }

//fun reverseGeocodeToPositions(lat: Double, lon: Double,
//                              limitNumeric: Int, limitRadius: Int,
//                              desiredLayers: List<Layer>) {
//}
//
//    fun reverseGeocode(lat: Double, lon: Double, limit: Int = 0, roadOnly: Boolean = true): ArrayList<Result> {
//
//
//        // first find all ways which are nearby
//        val stmt = con.prepareStatement(sql)
//        stmt.setDouble(1, lon)
//        stmt.setDouble(2, lat)
//        stmt.setDouble(3, lon)
//        stmt.setDouble(4, lat)
//        stmt.setDouble(5, lon)
//        stmt.setDouble(6, lat)
//        stmt.setDouble(7, lon)
//        stmt.setDouble(8, lat)
//        if (limit != 0) {
//            stmt.setInt(9, limit)
//        }
//        val result = stmt.executeQuery()
//
//        val results = ArrayList<Result>()
//
//        while (result.next()) {
//            val rEntity = OsmWay.fillFromRow(result)
//            val closestPoint = JTGFactory.coordinateFromWkb(result.getBinaryStream("closest_point")) ?: continue
//            val r = Result(rEntity, closestPoint, result.getDouble("distance"), nameResolver.getName(rEntity))
//
//            results.add(r)
//
//            logger.debug { "Found result for rev. query: ${rEntity.id}, distance: ${r.distance}, closest point (lat, lon): ${closestPoint.y}, ${closestPoint.x}, name ${r.name}" }
//        }
//
//        stmt.close()
//        result.close()
//
//        return results
//    }


}