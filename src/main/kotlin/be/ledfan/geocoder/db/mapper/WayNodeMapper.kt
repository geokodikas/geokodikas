package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.entity.WayNode
import be.ledfan.geocoder.db.getLayer
import be.ledfan.geocoder.importer.Layer
import org.intellij.lang.annotations.Language

class WayNodeMapper(private val con: ConnectionWrapper) : Mapper<WayNode>(con) {

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

    fun getLinkedWaysByNodes(osmNodes: List<OsmNode>): HashMap<Long, ArrayList<OsmWay>> = getLinkedWaysByNodesIds(osmNodes.map { it.id })

    fun getLinkedWaysByNodesIds(osmNodes: List<Long>): HashMap<Long, ArrayList<OsmWay>> {

        val sql = "SELECT node_id, way_id, way_layer FROM way_node WHERE node_id = ANY(?)"
        val array = con.createArrayOf("BIGINT", osmNodes.toTypedArray())
        val stmt = con.prepareStatement(sql)
        stmt.setArray(1, array)
        val result = stmt.executeQuery()

        val r = HashMap<Long, ArrayList<OsmWay>>() // node_id -> list of <way_ids,way_layer>
        osmNodes.forEach { r[it] = ArrayList() }

        while (result.next()) {
            val way = OsmWay.create(result.getLong("way_id"), result.getLayer("way_layer"))

            val nodeId = result.getLong("node_id")
            r[nodeId]?.add(way)
        }

        stmt.close()
        result.close()
        return r
    }

    fun getLinkedNodesByWay(osmWays: MutableCollection<OsmWay>): Map<Long, List<OsmNode>> {
        @Language("SQL")
        val sql = """SELECT way_id, node_id, node_layer, "order" from way_node where way_id= ANY(?)"""
        val stmt = con.prepareStatement(sql)
        val array = con.createArrayOf("BIGINT", osmWays.map { it.id }.toTypedArray())
        stmt.setArray(1, array)

        val result = stmt.executeQuery()

        val r = HashMap<Long, ArrayList<Pair<Int, OsmNode>>>()

        osmWays.forEach { r[it.id] = ArrayList() }

        while (result.next()) {
            val rel = OsmNode.create(result.getLong("node_id"), result.getLayer("node_layer"))

            val childId = result.getLong("way_id")
            r[childId]?.add(Pair(result.getInt("order"), rel))
        }

        stmt.close()
        result.close()

        return r.mapValues { (_, nodes) -> nodes.sortedBy { it.first }.map { it.second } }
    }

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
