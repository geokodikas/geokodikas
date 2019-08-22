package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.getLayer
import be.ledfan.geocoder.importer.Layer
import org.intellij.lang.annotations.Language

class OsmParentMapper(private val con: ConnectionWrapper) {

    fun getParents(entities: List<OsmEntity>): HashMap<Long, ArrayList<OsmRelation>> {
        @Language("SQL")
        val stmt = con.prepareCall("""
            SELECT child, osm_relation.osm_id, osm_relation.name, osm_relation.layer
            FROM osm_relation
               JOIN (SELECT DISTINCT p1.child_id                                                                  AS child,
                      unnest(array_cat(array_agg(DISTINCT p1.parent_id), array_agg(p2.parent_id))) AS parent_id
               FROM parent AS p1
                        LEFT OUTER JOIN parent AS p2 ON p1.parent_id = p2.child_id
               WHERE p1.child_id = ANY(?)
               GROUP BY (p1.child_id)) AS cpi ON osm_relation.osm_id = parent_id
        """.trimIndent())

        val array = con.createArrayOf("BIGINT", entities.map { it.id }.toTypedArray())
        stmt.setArray(1, array)

        val result = stmt.executeQuery()

        val r = HashMap<Long, ArrayList<OsmRelation>>()

        entities.forEach { r[it.id] = ArrayList() }

        while (result.next()) {
            val rel = OsmRelation.create(result.getLong("osm_id"), result.getString("name"), result.getLayer())

            val childId = result.getLong("child")
            r[childId]?.add(rel)
        }

        stmt.close()
        result.close()

        return r
    }

}
