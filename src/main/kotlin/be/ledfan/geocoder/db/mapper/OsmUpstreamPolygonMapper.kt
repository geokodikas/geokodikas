package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.entity.OsmUpstreamElement
import java.sql.Connection

class OsmUpstreamPolygonMapper(private val con: Connection) : Mapper<OsmUpstreamElement>(con) {

    override val entityCompanion = OsmUpstreamElement.Companion

    override val tableName = "osm_up_polygon"

}
