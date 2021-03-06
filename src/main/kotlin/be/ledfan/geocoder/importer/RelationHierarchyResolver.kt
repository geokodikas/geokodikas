package be.ledfan.geocoder.importer

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.importer.steps.executeBatchQueries
import be.ledfan.geocoder.importer.steps.executeBatchQueriesParallel
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import java.sql.Connection

class RelationHierarchyResolver(private val con: ConnectionWrapper) {

    private var logger = KotlinLogging.logger {}

    suspend fun run() {

        // Setup parents for relations
        // A relation (i.e. layer_order < 98) has the full hierarchy of parents stored and not only the first in the hierarchy
        @Language("SQL")
        val sql1 = """
            INSERT INTO parent(child_id, child_layer, child_osm_type, parent_id, parent_layer, parent_osm_type)
            ( SELECT child.osm_id, child.layer, 'relation', parent.osm_id, parent.layer, 'relation'
              FROM osm_relation AS child
               JOIN osm_relation AS parent
                ON st_contains(parent.geometry, child.geometry)
                AND parent.osm_id <> child.osm_id
                AND child.layer_order > parent.layer_order
                AND child.layer_order < 98 -- only setup relations for other relations
            )"""

        // Setup parents for nodes which lie within a Neighbourhood
        // Ways only have the neighbourhood (if any) and LocalAdmin stored
        // This query thus will setup Neighbourhood
        @Language("SQL")
        val sql2 = """
            INSERT INTO parent(child_id, child_layer, child_osm_type, parent_id, parent_layer, parent_osm_type)
            ( SELECT child.osm_id, child.layer, 'node', parent.osm_id, parent.layer, 'relation'
              FROM osm_node AS child
               JOIN osm_relation AS parent ON st_contains(parent.geometry, child.centroid)
               WHERE parent.layer = 'Neighbourhood'::Layer
            )"""

        // Setup parents for nodes which do not lie within a Neighbourhood
        // Ways only have the neighbourhood (if any) and LocalAdmin stored
        // This query thus will setup LocalAdmin
        @Language("SQL")
        val sql3 = """
            INSERT INTO parent(child_id, child_layer, child_osm_type, parent_id, parent_layer, parent_osm_type)
            ( SELECT child.osm_id, child.layer, 'node', parent.osm_id, parent.layer, 'relation'
              FROM osm_node AS child
               JOIN osm_relation AS parent ON st_contains(parent.geometry, child.centroid)
               WHERE parent.layer = 'LocalAdmin'::Layer
             )"""

        // Setup LocalAdmin parents for ways
        // Ways only have the neighbourhood (if any) and LocalAdmin stored
        // It is possible for ways to lie in multiple Neighbourhoods or LocalAdmins
        // This query thus will setup Neighbourhood
        @Language("SQL")
        val sql4 = """
            INSERT INTO parent(child_id, child_layer, child_osm_type, parent_id, parent_layer, parent_osm_type)
            ( SELECT child.osm_id, child.layer, 'way', parent.osm_id, parent.layer, 'relation'
              FROM osm_way AS child
               JOIN osm_relation AS parent ON st_intersects(parent.geometry, child.geometry)
               WHERE parent.layer = 'Neighbourhood'::Layer
            )"""


        // Setup LocalAdmin parents for ways
        // Ways only have the neighbourhood (if any) and LocalAdmin stored
        // It is possible for ways to lie in multiple Neighbourhoods or LocalAdmins
        // This query thus will setup LocalAdmin
        @Language("SQL")
        val sql5 = """
            INSERT INTO parent(child_id, child_layer, child_osm_type, parent_id, parent_layer, parent_osm_type)
            ( SELECT child.osm_id, child.layer, 'way', parent.osm_id, parent.layer, 'relation'
              FROM osm_way AS child
                JOIN osm_relation AS parent ON st_intersects(parent.geometry, child.geometry)
                WHERE parent.layer = 'LocalAdmin'::Layer
            )"""


        @Language("SQL")
        val sql6 = """INSERT INTO parent(child_id, child_layer, child_osm_type, parent_id, parent_layer, parent_osm_type)
        (SELECT child.child_id, child.child_layer, child.child_osm_type, parent.parent_id, parent.parent_layer, 'relation'
        FROM parent AS child
                JOIN parent ON child.parent_id = parent.child_id
                WHERE child.parent_layer = 'LocalAdmin'::Layer
                AND child.child_layer <> 'Neighbourhood'::Layer
        )"""


        val queries = listOf(sql1, sql2, sql3, sql4, sql5)

        logger.info("Going to run PIP queries, this may take some time")
        executeBatchQueriesParallel(queries)
        logger.info("Going to run final PIP query this may take some time")
        executeBatchQueries(listOf(sql6))

    }

}