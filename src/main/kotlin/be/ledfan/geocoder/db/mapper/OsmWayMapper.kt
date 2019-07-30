package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.entity.OsmWay
import java.sql.Connection
import java.util.*

class OsmWayMapper(private val con: Connection) : Mapper<OsmWay>(con) {

    override val entityCompanion = OsmWay.Companion

    override val tableName = "osm_way"

    fun bulkInsert(dbObjects: ArrayList<OsmWay>) {
        val stmt = con.prepareStatement("INSERT INTO osm_way (osm_id, geometry, version, tags, z_order, layer, layer_order, has_one_way_restriction, has_reversed_oneway) VALUES (?, ST_SetSRID(?, 4326), ?, ?, ?, ?::Layer, ?, ?, ?)")

        for (dbObject in dbObjects) {
            stmt.run {
                setLong(1, dbObject.id)
                setObject(2, dbObject.geometry)
                setInt(3, dbObject.version)
                setObject(4, dbObject.tags)
                setInt(5, dbObject.zOrder)
                setString(6, dbObject.layer.name)
                setInt(7, dbObject.layer.order)
                setBoolean(8, dbObject.hasOneWayRestriction)
                setBoolean(9, dbObject.hasReversedOneWay)
                addBatch()
            }
        }

        stmt.executeBatch()
        stmt.close()
    }

    fun getOneWaysWithNodes(): ArrayList<Pair<OsmWay, Array<Long>>> {
        val sql = "SELECT *, osm_up_ways.nodes as nodes FROM osm_way JOIN osm_up_ways ON osm_way.osm_id = osm_up_ways.id WHERE has_one_way_restriction = TRUE"

        val stmt = con.prepareStatement(sql)
        val result = stmt.executeQuery()

        val r = ArrayList<Pair<OsmWay, Array<Long>>>()

        while (result.next()) {
            r.add(Pair(entityCompanion.fillFromRow(result), result.getArray("nodes").array as Array<Long>))
        }

        stmt.close()
        result.close()
        return r
    }

}
