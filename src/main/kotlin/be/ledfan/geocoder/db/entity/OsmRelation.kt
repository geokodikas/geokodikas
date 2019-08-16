package be.ledfan.geocoder.db.entity

import be.ledfan.geocoder.db.getHstore
import be.ledfan.geocoder.importer.Layer
import org.postgis.PGgeometry
import java.sql.ResultSet

class OsmRelation(id: Long) : OsmEntity(id) {

    companion object : EntityCompanion<OsmRelation> {
        override fun fillFromRow(row: ResultSet): OsmRelation {
            val r = OsmRelation(row.getLong("osm_id"))

            r.version = row.getInt("version")
            r.geometry = row.getObject("geometry") as PGgeometry
            r.tags = row.getHstore("tags")
            r.zOrder = row.getInt("z_order")
            r.layer = Layer.valueOf(row.getString("layer"))
            r.name = row.getString("name")

            return r
        }

    }

    lateinit var geometry: PGgeometry
    var name: String? = null // nullable

}