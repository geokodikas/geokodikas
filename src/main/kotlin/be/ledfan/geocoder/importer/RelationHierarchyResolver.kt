package be.ledfan.geocoder.importer

import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import java.sql.Connection

class RelationHierarchyResolver(private val con: Connection) {

    private var logger = KotlinLogging.logger {}

    fun run() {
        @Language("SQL")
        val sql1 = """
            INSERT INTO parent(child_id, child_layer, child_osm_type, parent_id, parent_layer, parent_osm_type)
            ( SELECT child.osm_id, child.layer, 'relation', parent.osm_id, parent.layer, 'relation'
              FROM osm_relation AS child
               JOIN osm_relation AS parent
                ON st_contains(parent.geometry, child.geometry)
                AND parent.osm_id <> child.osm_id
                AND child.layer_order > parent.layer_order
                AND child.layer <> 'VirtualTrafficFlow'::Layer
            )"""

        @Language("SQL")
        val sql2 = """
            INSERT INTO parent(child_id, child_layer, child_osm_type, parent_id, parent_layer, parent_osm_type)
            ( SELECT child.osm_id, child.layer, 'node', parent.osm_id, parent.layer, 'relation'
              FROM osm_node AS child
               JOIN osm_relation AS parent ON st_contains(parent.geometry, child.centroid)
               WHERE parent.layer = 'Neighbourhood'::Layer
            )"""

        @Language("SQL")
        val sql3 = """
            INSERT INTO parent(child_id, child_layer, child_osm_type, parent_id, parent_layer, parent_osm_type)
            ( SELECT child.osm_id, child.layer, 'node', parent.osm_id, parent.layer, 'relation'
              FROM osm_node AS child
               JOIN osm_relation AS parent ON st_contains(parent.geometry, child.centroid)
               WHERE parent.layer = 'LocalAdmin'::Layer
                AND NOT EXISTS (SELECT * FROM parent WHERE child_id=child.osm_id AND parent_layer='Neighbourhood'::Layer)
             )"""

        @Language("SQL")
        val sql4 = """
            INSERT INTO parent(child_id, child_layer, child_osm_type, parent_id, parent_layer, parent_osm_type)
            ( SELECT child.osm_id, child.layer, 'way', parent.osm_id, parent.layer, 'relation'
              FROM osm_way AS child
               JOIN osm_relation AS parent ON st_contains(parent.geometry, child.centroid)
               WHERE parent.layer = 'Neighbourhood'::Layer
            )"""

        @Language("SQL")
        val sql5 = """
            INSERT INTO parent(child_id, child_layer, child_osm_type, parent_id, parent_layer, parent_osm_type)
            ( SELECT child.osm_id, child.layer, 'way', parent.osm_id, parent.layer, 'relation'
              FROM osm_way AS child
              JOIN osm_relation AS parent ON st_contains(parent.geometry, child.centroid)
              WHERE parent.layer = 'LocalAdmin'::Layer
               AND NOT EXISTS(SELECT * FROM parent WHERE child_id = child.osm_id AND parent_layer = 'Neighbourhood'::Layer)
            )"""

        val queries = listOf(sql1, sql2, sql3, sql4, sql5)

        for (query in queries) {
            logger.info("Performing query to calculate parents (PIP) this may take 1-2minutes.")
            val stmt = con.prepareStatement(query)
            stmt.executeUpdate()
            stmt.close()
            logger.info("Query executed")
        }

    }


}