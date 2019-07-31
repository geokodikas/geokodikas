package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.entity.OsmRelation
import java.sql.Connection
import java.util.*

class OsmRelationMapper(private val con: Connection) : Mapper<OsmRelation>(con) {

    override val entityCompanion = OsmRelation.Companion

    override val tableName = "osm_relation"

    fun bulkInsert(dbObjects: ArrayList<OsmRelation>) {
        val stmt = con.prepareStatement("INSERT INTO osm_relation (name, osm_id, version, tags, z_order, layer, layer_order, geometry, centroid) VALUES (?, ?, ?, ?, ?, ?::Layer, ?, ?, ST_SetSRID(st_centroid(?), 4326))")

        for (dbObject in dbObjects) {
            stmt.run {
                setString(1, dbObject.name)

                setLong(2, dbObject.id)
                setInt(3, dbObject.version)
                setObject(4, dbObject.tags)
                setInt(5, dbObject.zOrder)
                setString(6, dbObject.layer.name)
                setInt(7, dbObject.layer.order)
                setObject(8, dbObject.geometry)
                setObject(9, dbObject.geometry)

                addBatch()
            }
        }

        stmt.executeBatch()
        stmt.close()
    }
}
