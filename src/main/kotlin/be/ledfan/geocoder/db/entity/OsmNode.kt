package be.ledfan.geocoder.db.entity

import be.ledfan.geocoder.db.getCentroid
import be.ledfan.geocoder.db.getHstore
import be.ledfan.geocoder.db.getLayer
import be.ledfan.geocoder.importer.Layer
import org.postgis.PGgeometry
import java.sql.ResultSet

class OsmNode(id: Long) : OsmEntity(id) {

    override val Type = OsmType.Node

    companion object : EntityCompanion<OsmNode> {
        override fun fillFromRow(row: ResultSet): OsmNode {
            val r = OsmNode(row.getLong("osm_id"))

            r.version = row.getInt("version")
            row.getCentroid(r)
            r.tags = row.getHstore("tags")
            r.zOrder = row.getInt("z_order")
            r.layer = row.getLayer()

            return r
        }

        fun create(id: Long, layer: Layer): OsmNode {
            val r = OsmNode(id)

            r.layer = layer

            return r
        }
    }

    override fun mainGeometry(): PGgeometry = centroid

    lateinit var centroid: PGgeometry


}