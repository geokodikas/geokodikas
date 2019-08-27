package be.ledfan.geocoder.db.entity

import be.ledfan.geocoder.db.getHstore
import be.ledfan.geocoder.db.getLayer
import be.ledfan.geocoder.geocoding.SearchTable
import be.ledfan.geocoder.importer.Layer
import org.postgis.PGgeometry
import java.sql.ResultSet

class OsmRelation(id: Long) : OsmEntity(id) {

    override val Type = SearchTable.Relation

    companion object : EntityCompanion<OsmRelation> {
        override fun fillFromRow(row: ResultSet): OsmRelation {
            val r = OsmRelation(row.getLong("osm_id"))

            r.version = row.getInt("version")
            r.geometry = row.getObject("geometry") as PGgeometry
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

    }

    lateinit var geometry: PGgeometry
    var name: String? = null // nullable

}