package be.ledfan.geocoder.db.entity

import be.ledfan.geocoder.db.getGeometry
import be.ledfan.geocoder.db.getHstore
import be.ledfan.geocoder.db.getLayer
import be.ledfan.geocoder.importer.Layer
import org.postgis.PGgeometry
import java.sql.ResultSet

class OsmRelation(id: Long) : OsmEntity(id) {

    override val Type = OsmType.Relation

    companion object : EntityCompanion<OsmRelation> {
        override fun fillFromRow(row: ResultSet): OsmRelation {
            val r = OsmRelation(row.getLong("osm_id"))

            r.version = row.getInt("version")
            row.getGeometry(r)
            r.tags = row.getHstore("tags")
            r.zOrder = row.getInt("z_order")
            r.layer = row.getLayer()
            r.name = row.getString("name")

            return r
        }

        fun create(id: Long, name: String, layer: Layer): OsmRelation {
            val r = OsmRelation(id)
            r.name = name
            r.layer = layer
            return r
        }

        fun create(id: Long, tags: HashMap<String, String>, layer: Layer): OsmRelation {
            val r = OsmRelation(id)
            r.tags = tags
            r.layer = layer
            return r
        }

    }

    lateinit var geometry: PGgeometry

    var name: String? = null // nullable

    override fun mainGeometry(): PGgeometry = geometry

}