package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.entity.OsmUpstreamElement
import mu.KotlinLogging
import java.sql.Connection

class OsmUpstreamLineMapper(con: Connection) : Mapper(con) {

    override val entityCompanion = OsmUpstreamElement.Companion

    override val tableName = "osm_up_line"

}
