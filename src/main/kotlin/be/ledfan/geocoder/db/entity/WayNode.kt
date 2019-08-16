package be.ledfan.geocoder.db.entity

import be.ledfan.geocoder.importer.Layer
import java.sql.ResultSet

class WayNode : Entity {

    companion object : EntityCompanion<WayNode> {

        override fun fillFromRow(row: ResultSet): WayNode {
            val r = WayNode()

            r.wayId = row.getLong("way_id")
            r.nodeId = row.getLong("node_id")
            r.wayLayer = Layer.valueOf(row.getString("way_layer"))
            r.nodeLayer = Layer.valueOf(row.getString("node_layer"))

            return r
        }

        fun create(wayId: Long, nodeId: Long): WayNode {
            val r = WayNode()

            r.wayId = wayId
            r.nodeId = nodeId

            return r
        }


    }

    var wayId: Long = 0
    lateinit var wayLayer: Layer
    var nodeId: Long = 0
    lateinit var nodeLayer: Layer
    var fraction : Double? = null


}