package be.ledfan.geocoder.importer

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.mapper.OsmRelationMapper
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import java.sql.Connection

class RelationPostProcessor(private val config: Config,
                            private val con: Connection,
                            private val osmRelationMapper: OsmRelationMapper) {

    private val logger = KotlinLogging.logger {}

    fun setCentroid() {

        @Language("SQL")
        val sql = "UPDATE osm_relation SET centroid=st_centroid(geometry)"
        val stmt = con.prepareStatement(sql)
        stmt.executeUpdate()
        stmt.close()

        logger.info { "Updated centroids of osm_relation" }
    }

    fun prune() {

        @Language("SQL")
        val sql = """
            SELECT osm_id
             FROM osm_relation AS o
             WHERE o.centroid IS NOT NULL
              AND osm_id <> 52411
              AND NOT st_contains((SELECT geometry FROM osm_relation WHERE osm_id = ?).geometry, st_centroid(o.centroid));
            """

        val stmt = con.prepareStatement(sql)
        stmt.setLong(1, config.importer.countryId)
        val result = stmt.executeQuery()

        val ids = ArrayList<Long>()

        while (result.next()) {
            ids.add(result.getLong("osm_id"))
        }
        result.close()
        stmt.close()

        logger.info { "Pruning ${ids.size} relations" }
        logger.debug { "Pruned relations with id ${ids.joinToString()}" }

        osmRelationMapper.deleteByPrimaryIds(ids)
    }

}
