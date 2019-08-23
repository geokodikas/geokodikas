package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.OsmWay
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import java.util.*

class OsmWayMapper(private val con: ConnectionWrapper) : Mapper<OsmWay>(con) {

    override val entityCompanion = OsmWay.Companion

    override val tableName = "osm_way"

    private val logger = KotlinLogging.logger {}

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


    /**
     * doc preconditions
     */
    fun getStreetsForNodes_FilterByWayNameAndLocalAdmin(nodeIds: List<Long>): HashMap<Long, Long?> {

        logger.debug { "Finding related streets for ${nodeIds.size} nodes" }

        @Language("SQL")
        val sql = """WITH resolved_data AS (
    SELECT osm_node.osm_id              AS node_osm_id
         , centroid
         , regexp_replace(unaccent(lower(osm_way.tags -> 'addr:street')), '[\s[:punct:]]', '', 'g') AS street_name
         , parent.parent_id             AS localadmin_id
         , parent_layer
    FROM osm_node
             JOIN parent ON parent.child_id = osm_node.osm_id AND parent.parent_layer = 'LocalAdmin'::Layer
        WHERE osm_node.osm_id = ANY(?))
        SELECT node_osm_id AS address_osm_id,
               (SELECT way_id
                FROM streets
                    WHERE resolved_data.localadmin_id = streets.localadmin_id
                         AND resolved_data.street_name = streets.street_name
                   ORDER BY st_distance(resolved_data.centroid, streets.geometry)
                   LIMIT 1) AS street_osm_id
        FROM resolved_data"""

        val r = processStreetsForNodes(nodeIds, sql)

        logger.debug { "Done processing streets for ${nodeIds.size} nodes" }

        return r
    }

    private fun processStreetsForNodes(nodeIds: List<Long>, sql: String): HashMap<Long, Long?> {
        val stmt = con.prepareStatement(sql)
        val array = con.createArrayOf("BIGINT", nodeIds.toTypedArray())
        stmt.setArray(1, array)

        val result = stmt.executeQuery()
        val r = HashMap<Long, Long?>()
        nodeIds.forEach { r[it] = null }

        while (result.next()) {
            r[result.getLong("address_osm_id")] = result.getLong("street_osm_id")
        }

        stmt.close()
        result.close()

        return r
    }

    fun getStreetsForNodes_FilterByClosestAndLocalAdmin(nodeIds: ArrayList<Long>): Map<out Long, Long?> {

        logger.debug { "Finding related streets for ${nodeIds.size} nodes (by closest street)" }

        @Language("SQL")
        val sql = """WITH resolved_data AS (
                SELECT osm_node.osm_id  AS node_osm_id
                     , centroid
                     , parent.parent_id AS localadmin_id
                     , parent_layer
                FROM osm_node
                         JOIN parent ON parent.child_id = osm_node.osm_id AND parent.parent_layer = 'LocalAdmin'::Layer
                    WHERE osm_node.osm_id = ANY (?))
            SELECT node_osm_id as address_osm_id,
                   (SELECT way_id
                    FROM streets
                        WHERE resolved_data.localadmin_id = streets.localadmin_id
                        ORDER BY st_distance (resolved_data.centroid, streets.geometry)
                        LIMIT 1) AS street_osm_id
            FROM resolved_data
            """

        val r = processStreetsForNodes(nodeIds, sql)

        logger.debug { "Done processing streets for ${nodeIds.size} nodes" }

        return r
    }

    fun getAddressesAndVenues(): HashMap<Long, OsmWay> {
        @Language("SQL")
        val stmt = con.prepareCall("""
            SELECT *
            FROM $tableName
            WHERE layer = 'Address'::Layer OR layer='Venue'::Layer
        """.trimIndent())

        return executeSelect(stmt)
    }

    fun getStreetsForWays_FilterByWayNameAndLocalAdmin(waysIds: ArrayList<Long>): HashMap<Long, Long?> {

        logger.debug { "Finding related streets for ${waysIds.size} ways" }

        @Language("SQL")
        val sql = """WITH resolved_data AS (
    SELECT osm_way.osm_id              AS address_osm_id
         , geometry
         , regexp_replace(unaccent(lower(osm_way.tags -> 'addr:street')), '[\s[:punct:]]', '', 'g') AS street_name
         , parent.parent_id             AS localadmin_id
         , parent_layer
    FROM osm_way
             JOIN parent ON parent.child_id = osm_way.osm_id AND parent.parent_layer = 'LocalAdmin'::Layer
        WHERE osm_way.osm_id = ANY(?))
        SELECT address_osm_id,
               (SELECT way_id
                FROM streets
                    WHERE resolved_data.localadmin_id = streets.localadmin_id
                         AND resolved_data.street_name = streets.street_name
                   ORDER BY st_distance(resolved_data.geometry, streets.geometry)
                   LIMIT 1) AS street_osm_id
        FROM resolved_data"""

        val r = processStreetsForNodes(waysIds, sql)

        logger.debug { "Done processing streets for ${waysIds.size} ways" }

        return r
    }

    fun getStreetsForWays_FilterByClosestAndLocalAdmin(waysIds: ArrayList<Long>): Map<Long, Long?> {

        logger.debug { "Finding related streets for ${waysIds.size} ways  (by closest street)" }

        @Language("SQL")
        val sql = """WITH resolved_data AS (
    SELECT osm_way.osm_id              AS address_osm_id
         , geometry
         , parent.parent_id             AS localadmin_id
         , parent_layer
    FROM osm_way
             JOIN parent ON parent.child_id = osm_way.osm_id AND parent.parent_layer = 'LocalAdmin'::Layer
        WHERE osm_way.osm_id = ANY(?))
        SELECT address_osm_id,
               (SELECT way_id
                FROM streets
                    WHERE resolved_data.localadmin_id = streets.localadmin_id
                   ORDER BY st_distance(resolved_data.geometry, streets.geometry)
                   LIMIT 1) AS street_osm_id
        FROM resolved_data"""


        val r = processStreetsForNodes(waysIds, sql)

        logger.debug { "Done processing streets for ${waysIds.size} ways" }

        return r
    }
}
