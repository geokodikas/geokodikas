package be.ledfan.geocoder.db.entity

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.ConnectionWrapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKBReader


class Country(val id: Long, val name: String, private val geometry: Geometry) {

    private val factory = GeometryFactory()

    companion object {
        fun getFromDb(con: ConnectionWrapper, config: Config): Country {
            val sql = "SELECT name, st_asbinary(geometry) as geometry from osm_relation where layer='Country'::Layer and osm_id=? LIMIT 1"
            val stmt = con.prepareStatement(sql)
            stmt.setLong(1, config.importer.countryId)
            val result = stmt.executeQuery()
            result.next()

            val geometry = WKBReader().read(result.getBinaryStream("geometry").readBytes())
            if (geometry == null || geometry.geometryType != "MultiPolygon") {
                throw Exception("Could not find correct geometry for country ${config.importer.countryId}")
            }

            val name = result.getString("name")

            return Country(config.importer.countryId, name, geometry)
        }
    }

    fun containsNode(node: OsmNode): Boolean {
        val centroid = node.centroid.geometry as org.postgis.Point
        val point = factory.createPoint(Coordinate(centroid.x, centroid.y))
        return point.within(geometry)
    }

    fun containsWay(way: OsmWay): Boolean {
        val centroid = way.centroid.geometry as org.postgis.Point
        val point = factory.createPoint(Coordinate(centroid.x, centroid.y))
        return point.within(geometry)
    }



}