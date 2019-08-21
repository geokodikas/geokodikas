package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.OsmWay

class Reverse(private val con: ConnectionWrapper) {

    data class Result(val osmWay: OsmWay, val distance: Double, val name: String)

    fun reverseGeocode(lat: Double, lon: Double,
                       limitNumeric: Int?, limitRadius: Int?,
                       desiredLayers: List<String>?): ArrayList<Result> {

        val (sqlQuery, parameters) = ReverseQueryBuilder(debug=true).run {
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

        val stmt = con.prepareStatement(sqlQuery)

        parameters.forEachIndexed { index, param ->
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