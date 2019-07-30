package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.entity.OneWayRestriction
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.entity.WayNode
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.util.NoSuchElementException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.getOrPut
import kotlin.collections.toTypedArray

class OneWayRestrictionMapper(private val con: Connection) : Mapper<OneWayRestriction>(con) {

    override val entityCompanion = OneWayRestriction.Companion

    override val tableName = "one_way_restrictions"

    fun bulkInsert(dbObjects: ArrayList<OneWayRestriction>) {

        val stmt = con.prepareStatement("INSERT INTO one_way_restrictions (way_id, from_node_id, to_node_id) VALUES (?, ?, ?)")

        for (node in dbObjects) {
            stmt.run {
                stmt.setLong(1, node.wayId)
                stmt.setLong(2, node.fromNodeId)
                stmt.setLong(3, node.toNodeId)
                stmt.addBatch()
            }
        }

        stmt.executeBatch()
        stmt.close()
    }

}
