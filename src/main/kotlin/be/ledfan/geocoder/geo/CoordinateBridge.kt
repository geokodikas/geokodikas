package be.ledfan.geocoder.geo

import be.ledfan.geojsondsl.ICoordinate
import com.beust.klaxon.JsonArray
import com.beust.klaxon.json
import org.locationtech.jts.geom.Coordinate
import org.postgis.Point

data class PostGisCoordinateBridge(val p: Point) : ICoordinate {

    override fun toJson(): JsonArray<Double> {
        return json {
            array(
                    p.x,
                    p.y
            )
        } as JsonArray<Double>
    }

}

fun Point.toGeoJsonCoordinate(): ICoordinate {
    return PostGisCoordinateBridge(this)
}

data class JtsCoordinateBridge(val jtsCoordinate: Coordinate) : ICoordinate {

    override fun toJson(): JsonArray<Double> {
        return json {
            array(
                    jtsCoordinate.x,
                    jtsCoordinate.y
            )
        } as JsonArray<Double>
    }
}

fun Coordinate.toGeoJsonCoordinate(): ICoordinate {
    return JtsCoordinateBridge(this)
}
