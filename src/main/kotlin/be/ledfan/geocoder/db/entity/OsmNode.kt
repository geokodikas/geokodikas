package be.ledfan.geocoder.db.entity

import be.ledfan.geocoder.db.getHstore
import be.ledfan.geocoder.db.getLayer
import be.ledfan.geocoder.importer.Layer
import org.postgis.PGgeometry
import java.sql.ResultSet

class OsmNode(id: Long) : OsmEntity(id) {

    companion object : EntityCompanion<OsmNode> {
        override fun fillFromRow(row: ResultSet): OsmNode {
            val r = OsmNode(row.getLong("osm_id"))

            r.version = row.getInt("version")
            r.centroid = row.getObject("centroid") as PGgeometry
            r.tags = row.getHstore("tags")
            r.zOrder = row.getInt("z_order")
            r.layer = row.getLayer()

            return r
        }
    }

    lateinit var centroid: PGgeometry


}