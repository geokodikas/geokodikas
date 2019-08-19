package be.ledfan.geocoder.geo

import be.ledfan.geojsondsl.GeometryFactory
import org.postgis.*


fun Point.toGeoJsonCoordinate(): CoordinateBridge {
    return CoordinateBridge(this)
}

fun PGgeometry.toGeoJson(geometryFactory: GeometryFactory) {
    val self = this
    geometryFactory.apply {
        when (geoType) {
            Geometry.LINESTRING -> {
                (self.geometry as LineString).toGeoJson(this)
            }
            Geometry.POLYGON -> {
                (self.geometry as Polygon).toGeoJson(this)
            }
        }
    }
}

fun Polygon.toGeoJson(geometryFactory: GeometryFactory) {
    geometryFactory.apply {
        polygon {
            for (i in 0 until numRings()) {
                ring {
                    getRing(i).points.forEach {
                        withCoordinate(it.toGeoJsonCoordinate())
                    }
                }
            }
        }
    }
}

fun LineString.toGeoJson(geometryFactory: GeometryFactory) {
    geometryFactory.apply {
        lineString {
            points.forEach {
                withCoordinate(it.toGeoJsonCoordinate())
            }
        }
    }
}
