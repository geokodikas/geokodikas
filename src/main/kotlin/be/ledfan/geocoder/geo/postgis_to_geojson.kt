package be.ledfan.geocoder.geo

import be.ledfan.geojsondsl.GeometryFactory
import be.ledfan.geojsondsl.MultiPolygon as GeoMultiPolygon
import org.postgis.*



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
            Geometry.POINT -> {
                (self.geometry as Point).toGeoJson(this)
            }
            Geometry.MULTIPOLYGON -> {
                (self.geometry as MultiPolygon).toGeoJson(this)
            }
        }
    }
}

fun Polygon.toGeoJson(geometryFactory: GeoMultiPolygon) {
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

fun Point.toGeoJson(geometryFactory: GeometryFactory) {
    geometryFactory.apply {
        point(this@toGeoJson.toGeoJsonCoordinate())
    }
}

fun MultiPolygon.toGeoJson(geometryFactory: GeometryFactory) {
    geometryFactory.apply {
        multiPolygon {
            this@toGeoJson.polygons.forEach { polygon ->
                polygon.toGeoJson(this@multiPolygon)
            }
        }
    }
}


