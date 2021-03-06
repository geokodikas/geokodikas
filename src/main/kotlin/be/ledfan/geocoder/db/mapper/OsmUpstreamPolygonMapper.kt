package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.OsmUpstreamElement

class OsmUpstreamPolygonMapper(private val con: ConnectionWrapper) : Mapper<OsmUpstreamElement>(con) {

    override val entityCompanion = OsmUpstreamElement.Companion

    override val tableName = "osm_up_polygon"

}
