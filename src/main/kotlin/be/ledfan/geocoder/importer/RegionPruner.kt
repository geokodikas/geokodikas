package be.ledfan.geocoder.importer

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.mapper.OsmNodeMapper
import be.ledfan.geocoder.db.mapper.OsmRelationMapper
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import java.sql.Connection

class RegionPruner(private val config: Config,
                   private val con: Connection,
                   private val osmRelationMapper: OsmRelationMapper,
                   private val osmNodeMapper: OsmNodeMapper,
                   private val osmWayMapper: OsmWayMapper
                   ) {

    private val logger = KotlinLogging.logger {}

    fun prune() {
        pruneNodes()
        pruneWays()
        pruneRelations()
    }

    private fun pruneRelations() {
        @Language("SQL")
        val sql = """
            SELECT osm_id
             FROM osm_relation AS o
             WHERE o.centroid IS NOT NULL
              AND osm_id <> 52411
              AND NOT st_contains((SELECT geometry FROM osm_relation WHERE osm_id = ?).geometry, ST_PointOnSurface(o.geometry));
            """

        val ids = collect(sql)

        logger.info { "Pruning ${ids.size} relations" }
        logger.debug { "Pruned relations with id ${ids.joinToString()}" }

        osmRelationMapper.deleteByPrimaryIds(ids)
    }

    private fun pruneNodes() {
        @Language("SQL")
        val sql = """
            SELECT osm_id
            FROM (SELECT *
                  FROM osm_node
                  where not exists(SELECT * FROM parent WHERE child_id = osm_node.osm_id)
                 ) as nodes,
                 (SELECT geometry
                  FROM osm_relation elation
                  WHERE osm_id = ?) as belgium
            WHERE NOT st_intersects(nodes.centroid, belgium.geometry)
              """

        val ids = collect(sql)

        logger.info { "Pruning ${ids.size} nodes" }
        logger.debug { "Pruned nodes with id ${ids.joinToString()}" }

        osmNodeMapper.deleteByPrimaryIds(ids)
    }

    private fun pruneWays() {
        @Language("SQL")
        val sql = """
            SELECT osm_id
            FROM osm_way AS o
              WHERE NOT (SELECT geometry FROM osm_relation WHERE osm_id = 52411).geometry && o.geometry
                    AND NOT st_intersects((SELECT geometry FROM osm_relation WHERE osm_id = ?)  .geometry, o.geometry)
              """

        val ids = collect(sql)

        logger.info { "Pruning ${ids.size} ways" }
        logger.debug { "Pruned ways with id ${ids.joinToString()}" }

        osmWayMapper.deleteByPrimaryIds(ids)
    }


    private fun collect(query: String): ArrayList<Long> {
        val stmt = con.prepareStatement(query)
        stmt.setLong(1, config.importer.countryId)
        val result = stmt.executeQuery()

        val ids = ArrayList<Long>()

        while (result.next()) {
            ids.add(result.getLong("osm_id"))
        }
        result.close()
        stmt.close()

        return ids
    }

}
