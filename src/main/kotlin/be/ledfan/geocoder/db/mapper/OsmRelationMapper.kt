package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.getHstore
import be.ledfan.geocoder.db.getLayer
import be.ledfan.geocoder.importer.Layer
import org.intellij.lang.annotations.Language
import java.util.*

class OsmRelationMapper(private val con: ConnectionWrapper) : Mapper<OsmRelation>(con) {

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

    fun getParents(relation: OsmRelation): Map<Layer, OsmRelation> = getParents(relation.id)

    fun getParents(relationId: Long): Map<Layer, OsmRelation> {
        @Language("SQL")
        val stmt = con.prepareCall("""
            SELECT osm_relation.*
            FROM osm_relation
            WHERE osm_id IN (
                SELECT unnest(array_cat(array_agg(DISTINCT p1.parent_id), array_agg(p2.parent_id))) AS parents
                FROM parent AS p1
                         RIGHT OUTER JOIN parent AS p2 ON p1.parent_id = p2.child_id
                WHERE p1.child_id = ?);
        """.trimIndent())

        stmt.setLong(1, relationId)

        return executeSelect(stmt).toList().associateBy({ it.second.layer }, { it.second })
    }

    fun getByLayer(layer: Layer): HashMap<Long, OsmRelation> {
        @Language("SQL")
        val stmt = con.prepareCall("""
            SELECT *
            FROM osm_relation
            WHERE layer = ?::Layer
        """.trimIndent())

        stmt.setString(1, layer.toString())

        return executeSelect(stmt)
    }

    /**
     * Fetch tags and layer
     */
    fun getSlimByPrimaryIds(ids: List<Long>): HashMap<Long, OsmRelation> {
        val sql = "SELECT osm_id, tags, layer FROM osm_relation WHERE osm_id = ANY(?)" // TODO replace osm_id
        val array = con.createArrayOf("BIGINT", ids.toTypedArray())
        val stmt = con.prepareStatement(sql)
        stmt.setArray(1, array)

        val result = stmt.executeQuery()

        val r = HashMap<Long, OsmRelation>()

        while (result.next()) {
            val rEntity = entityCompanion.create(result.getLong("osm_id"), result.getHstore("tags"), result.getLayer())
            r[result.getLong("osm_id")] = rEntity
        }

        stmt.close()
        result.close()
        return r

    }

}
