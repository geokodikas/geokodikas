package be.ledfan.geocoder.db.entity

import be.ledfan.geocoder.db.getHstore
import org.postgis.PGgeometry
import java.sql.ResultSet

class OsmUpstreamElement(val id: Long) : Entity {

    companion object : EntityCompanion<OsmUpstreamElement> {
        override fun fillFromRow(row: ResultSet): OsmUpstreamElement {
            val r = OsmUpstreamElement(row.getLong("osm_id"))

            r.way = row.getObject("way") as PGgeometry
            r.tags = row.getHstore("tags")
            r.zOrder = row.getInt("z_order")

            return r
        }

    }

    lateinit var way: PGgeometry

    var zOrder: Int = 0

    lateinit var tags: HashMap<String, String>
}