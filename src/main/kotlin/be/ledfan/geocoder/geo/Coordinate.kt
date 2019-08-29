package be.ledfan.geocoder.geo

import be.ledfan.geojsondsl.ICoordinate
import com.beust.klaxon.JsonArray
import com.beust.klaxon.json
import org.postgis.Point

data class Coordinate(val lon: Double, val lat: Double) : ICoordinate {

    override fun toJson(): JsonArray<Double> {
        return json {
            array(
                    lon,
                    lat
            )
        } as JsonArray<Double>
    }

}