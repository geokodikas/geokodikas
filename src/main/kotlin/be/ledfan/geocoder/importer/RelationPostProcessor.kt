package be.ledfan.geocoder.importer

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.mapper.OsmRelationMapper
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import java.sql.Connection

class RelationPostProcessor(private val config: Config,
                            private val con: ConnectionWrapper,
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

}
