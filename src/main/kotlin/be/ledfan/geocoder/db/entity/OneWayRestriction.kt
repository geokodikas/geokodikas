package be.ledfan.geocoder.db.entity

import java.sql.ResultSet

class OneWayRestriction : Entity {

    companion object : EntityCompanion<OneWayRestriction> {

        override fun fillFromRow(row: ResultSet): OneWayRestriction {
            val r = OneWayRestriction()

            r.wayId = row.getLong("way_id")
            r.fromNodeId = row.getLong("from_node_id")
            r.toNodeId = row.getLong("to_node_id")

            return r
        }

    }

    var wayId: Long = 0
    var fromNodeId: Long = 0
    var toNodeId: Long = 0

}