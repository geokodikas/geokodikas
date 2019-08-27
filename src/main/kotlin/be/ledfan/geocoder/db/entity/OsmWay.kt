package be.ledfan.geocoder.db.entity

//import be.ledfan.geocoder.geocoder.INameResolvable
import be.ledfan.geocoder.db.getHstore
import be.ledfan.geocoder.db.getLayer
import be.ledfan.geocoder.geocoding.SearchTable
import be.ledfan.geocoder.importer.Layer
import org.postgis.PGgeometry
import java.sql.ResultSet

class OsmWay(id: Long) : OsmEntity(id) {

    override val Type = SearchTable.Way

    companion object : EntityCompanion<OsmWay> {

        override fun fillFromRow(row: ResultSet): OsmWay {
            val r = OsmWay(row.getLong("osm_id"))

            r.version = row.getInt("version")
            r.geometry = row.getObject("geometry") as PGgeometry
            r.centroid = row.getObject("centroid") as PGgeometry
            r.tags = row.getHstore("tags")
            r.zOrder = row.getInt("z_order")
            r.layer = row.getLayer()
            r.hasOneWayRestriction = row.getBoolean("has_one_way_restriction")
            r.hasReversedOneWay = row.getBoolean("has_reversed_oneway")

            return r
        }

        fun create(id: Long, layer: Layer): OsmWay {
            val r = OsmWay(id)
            r.layer = layer

            return r
        }

    }

    lateinit var geometry: PGgeometry
    lateinit var centroid: PGgeometry
    var hasOneWayRestriction: Boolean = false
    var hasReversedOneWay: Boolean = false

}