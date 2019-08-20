package be.ledfan.geocoder.geo

import be.ledfan.geojsondsl.ICoordinate
import com.beust.klaxon.JsonArray
import com.beust.klaxon.json
import org.postgis.Point

data class CoordinateBridge(val p: Point) : ICoordinate {

    override fun toJson(): JsonArray<Double> {
        return json {
            array(
                    p.x,
                    p.y
            )
        } as JsonArray<Double>
    }

}