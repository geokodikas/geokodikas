package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.entity.OsmNode
import java.sql.Connection
import java.util.*

class OsmNodeMapper(private val con: Connection) : Mapper<OsmNode>(con) {

    override val entityCompanion = OsmNode.Companion

    override val tableName = "osm_node"

    fun bulkInsert(dbObjects: ArrayList<OsmNode>) {
        val stmt = con.prepareStatement("INSERT INTO osm_node (osm_id, centroid, version, tags, z_order, layer, layer_order) VALUES (?, ST_SetSRID(?, 4326), ?, ?, ?, ?::Layer, ?)")

        for (dbObject in dbObjects) {
            stmt.run {
                setLong(1, dbObject.id)
                setObject(2, dbObject.centroid)
                setInt(3, dbObject.version)
                setObject(4, dbObject.tags)
                setInt(5, dbObject.zOrder)
                setString(6, dbObject.layer.name)
                setInt(7, dbObject.layer.order)
                addBatch()
            }
        }

        stmt.executeBatch()
        stmt.close()
    }

}
