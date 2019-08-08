package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.entity.WayNode
import be.ledfan.geocoder.importer.Layer
import org.intellij.lang.annotations.Language
import java.sql.Connection

class WayNodeMapper(private val con: Connection) : Mapper<WayNode>(con) {

    override val entityCompanion = WayNode.Companion

    override val tableName = "way_node"

    data class BulkInsertData(val osmWay: OsmWay, val nodeId: Long, val order: Int)

    fun bulkInsert(dbObjects: ArrayList<BulkInsertData>) {
        val stmt = con.prepareStatement("""INSERT INTO way_node (way_id, way_layer, way_layer_order, node_id, node_layer, node_layer_order, fraction, "order") VALUES (?, ?::Layer, ?, ?, NULL, NULL, NULL, ?)""")

        for (data in dbObjects) {
            stmt.run {
                stmt.setLong(1, data.osmWay.id)
                stmt.setString(2, data.osmWay.layer.name)
                stmt.setInt(3, data.osmWay.layer.order)
                stmt.setLong(4, data.nodeId)
                stmt.setInt(5, data.order)
                stmt.addBatch()
            }
        }

        stmt.executeBatch()
        stmt.close()
    }

    fun bulkUpdateNodeLayer(dbObjects: ArrayList<WayNode>) {

        val stmt = con.prepareStatement("UPDATE way_node SET node_layer=?::Layer, node_layer_order=? WHERE way_id=? AND node_id=?")

        for (wayNode in dbObjects) {
            stmt.run {
                setString(1, wayNode.nodeLayer.name)
                setInt(2, wayNode.nodeLayer.order)
                setLong(3, wayNode.wayId)
                setLong(4, wayNode.nodeId)
                addBatch()
            }
        }
        stmt.executeBatch()
        stmt.close()

    }

    fun bulkUpdateFraction(dbObjects: ArrayList<WayNode>) {

        val stmt = con.prepareStatement("UPDATE way_node SET fraction=? WHERE way_id=? AND node_id=?")

        for (wayNode in dbObjects) {
            val fraction = wayNode.fraction
            if (fraction != null) {
                stmt.run {
                    setDouble(1, fraction)
                    setLong(2, wayNode.wayId)
                    setLong(3, wayNode.nodeId)
                    addBatch()
                }
            }
        }
        stmt.executeBatch()
        stmt.close()

    }

    fun getLinkedWaysByNode(osmIds: List<Long>): HashMap<Long, ArrayList<Pair<Long, Layer>>> {

        val sql = "SELECT * FROM $tableName WHERE node_id = ANY(?)"
        val array = con.createArrayOf("BIGINT", osmIds.toTypedArray())
        val stmt = con.prepareStatement(sql)
        stmt.setArray(1, array)
        val result = stmt.executeQuery()

        val r = HashMap<Long, ArrayList<Pair<Long, Layer>>>() // node_id -> list of <way_ids,way_layer>

        while (result.next()) {
            r.getOrPut(result.getLong("node_id")) { ArrayList() }.add(
                    Pair(result.getLong("way_id"), Layer.valueOf(result.getString("way_layer")))
            )
        }

        stmt.close()
        result.close()
        return r
    }

//    fun getLinkedWaysByNode(osmNodeId: Long): HashMap<Long, Array<Long>> {
//
//        @Language("SQL")
//        val sql = """SELECT *
//                     FROM (
//                             SELECT osm_way.osm_id                                   AS way_id,
//                                    osm_way.has_one_way_restriction,
//                                    CASE
//                                        WHEN osm_way.has_one_way_restriction THEN (
//                                            (SELECT array_agg(to_node_id)
//                                             FROM one_way_restrictions
//                                             WHERE way_id = osm_way.osm_id
//                                               AND from_node_id = wn1.node_id)
//                                        )
//                                        ELSE (SELECT array_agg(DISTINCT wn2.node_id)
//                                              FROM way_node AS wn2
//                                              WHERE wn2.way_id = osm_way.osm_id) END AS nodes_id
//                             FROM way_node AS wn1
//                                      JOIN osm_way ON osm_way.osm_id = wn1.way_id
//                             WHERE wn1.node_id = ?
//                               AND (wn1.way_layer = 'Street'::Layer OR wn1.way_layer = 'Junction'::Layer OR wn1.way_layer = 'Link'::Layer)
//                             GROUP BY (osm_way.osm_id, osm_way.has_one_way_restriction, wn1.node_id)) AS dummy
//                     WHERE nodes_id IS NOT NULL""";
//
//        val stmt = con.prepareStatement(sql)
//        stmt.setLong(1, osmNodeId)
//
//        val result = stmt.executeQuery()
//
//        val r = HashMap<Long, Array<Long>>()
//
//        while (result.next()) {
//            r[result.getLong("way_id")] = result.getArray("nodes_id").array as Array<Long>
//        }
//
//        stmt.close()
//        result.close()
//        return r
//    }

    fun pruneWithoutNodeLayer(): Int {
        val sql = "DELETE FROM $tableName WHERE node_layer IS NULL"
        val stmt = con.prepareStatement(sql)
        val result = stmt.executeUpdate()
        stmt.close()
        return result
    }


    fun pruneWayNodesRelatedToArealStreets(): Int {
        @Language("SQL")
        val sql = """
            DELETE FROM way_node
            WHERE way_layer = 'Street'::Layer
              AND node_layer IN ('Address'::Layer, 'Venue'::Layer)
              AND EXISTS(SELECT osm_id FROM osm_way WHERE osm_way.osm_id = way_node.way_id)
        """.trimIndent()
        val stmt = con.prepareStatement(sql)
        val result = stmt.executeUpdate()
        stmt.close()
        return result
    }

}
