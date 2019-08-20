package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.OsmWay
import com.vividsolutions.jts.geom.Coordinate

class Reverse(private val con: ConnectionWrapper) {

//    data class Result(val osmWay: OsmWay, val closestPoint: Coordinate, val name: String)
    data class Result(val osmWay: OsmWay, val distance: Double, val name: String)
//    data class Result(val osmWay: Long, val distance: Double, val name: String)

    fun reverseGeocode(lat: Double, lon: Double,
                       limitNumeric: Int?, limitRadius: Int?,
                       desiredLayers: List<String>?): ArrayList<Result> {

        val (sqlQuery, parameters) = ReverseQueryBuilder().run {
            baseQuery(lat, lon)
            if (limitRadius != null && limitRadius > 0) {
                whereMetricDistance(limitRadius)
            }
            if (desiredLayers != null) {
                whereLayer(desiredLayers)
            }
            orderBy()
            if (limitNumeric != null && limitNumeric > 0) {
                limit(limitNumeric)
            } else {
                limit(5)
            }
            build()
        }
        println("SQL QUERY: $sqlQuery")
        println("Paramters: $parameters")

        val stmt = con.prepareStatement(sqlQuery)

        parameters.forEachIndexed { index, param ->
            println("Set ${index + 1} => $param")
            stmt.setObject(index + 1, param)
        }

        val result = stmt.executeQuery()

        val results = ArrayList<Result>()

        while (result.next()) {
            val rEntity = OsmWay.fillFromRow(result)
            val r = Result(rEntity,  result.getDouble("distance"), "testname")
            results.add(r)
        }

        stmt.close()
        result.close()

        return results


    }

}