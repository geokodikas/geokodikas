@file:Suppress("FunctionName")

package be.ledfan.geocoder.importer.steps

import be.ledfan.geocoder.db.ConnectionFactory
import mu.KotlinLogging
import org.intellij.lang.annotations.Language

private val con = ConnectionFactory.createConnection()
private val logger = KotlinLogging.logger {}

suspend fun step0_checks(): Boolean {

    return (ensureNotEmpty("osm_up_point")
            && ensureNotEmpty("osm_up_line")
            && ensureNotEmpty("osm_up_polygon"))
}

suspend fun step0_create_indexes(): Boolean {

    // create indexes on upstream tables

    @Language("SQL")
    val sqlQueries = listOf(
            "CREATE INDEX IF NOT EXISTS osm_up_line_osm_id_index ON osm_up_line (osm_id)",
            "CREATE INDEX IF NOT EXISTS osm_up_point_osm_id_index ON  osm_up_point (osm_id)",
            "CREATE INDEX IF NOT EXISTS osm_up_polygon_osm_id_index ON osm_up_polygon (osm_id)")

    return executeBatchQueries(sqlQueries)
}


suspend fun step0_create_schema(): Boolean {

    @Language("SQL")
    val sqlQueries = listOf("""
            DO $$
            BEGIN
                IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'layer' and typinput = 'enum_in'::regproc) THEN
                    CREATE TYPE Layer AS ENUM (
                      'VirtualTrafficFlow',
                      'PhysicalTrafficFlow',
                      'Junction',
                      'Link',
                      'Venue',
                      'Address',
                      'Street',
                      'Neighbourhood',
                      'LocalAdmin',
                      'County',
                      'Region',
                      'MacroRegion',
                      'Country',
                      'Superfluous'
                    );
                END IF;
            END$$;
              """,
            """
            CREATE TABLE IF NOT EXISTS osm_node
            (
              osm_id      bigint                NOT NULL,
              centroid    geometry(Point, 4326) NOT NULL,
              version     int,
              tags        hstore,
              z_order     integer,
              layer       Layer                 NOT NULL,
              layer_order int                   NOT NULL
            );""",

            """
            CREATE TABLE IF NOT EXISTS osm_way
            (
                osm_id                  bigint   NOT NULL,
                geometry                geometry NOT NULL,
                centroid                geometry(Point, 4326),
                version                 int,
                tags                    hstore,
                z_order                 integer,
                layer                   Layer    NOT NULL,
                layer_order             int      NOT NULL,
                has_one_way_restriction boolean  NOT NULL,
                has_reversed_oneway     boolean  NOT NULL
            );""",

            """
            CREATE TABLE IF NOT EXISTS way_node
            (
              way_id           bigint NOT NULL,
              way_layer        Layer  NOT NULL,
              way_layer_order  int    NOT NULL,
              node_id          bigint NOT NULL,
              node_layer       Layer  , -- null during first processing of ways
              node_layer_order int    , -- null during first processing of ways,
              fraction         double precision, -- null during first processing of ways,
              "order"          int    NOT null
            );""",

            """
            CREATE TABLE IF NOT EXISTS osm_relation
            (
              osm_id bigint NOT NULL,
              geometry geometry,
              version integer,
              tags hstore,
              z_order integer,
              layer layer NOT NULL,
              layer_order integer NOT NULL,
              name varchar(255),
              centroid geometry(Point,4326)
            );""",


            """
            CREATE TABLE IF NOT EXISTS parent
            (
                child_id bigint NOT NULL,
                child_layer layer NOT NULL,
                child_osm_type varchar(255) NOT NULL,
                parent_id bigint NOT NULL,
                parent_layer layer NOT NULL,
                parent_osm_type varchar(255) NOT NULL
            );""",

            """
            CREATE TABLE IF NOT EXISTS one_way_restrictions
            (
                way_id       bigint NOT NULL,
                from_node_id bigint NOT NULL,
                to_node_id   bigint NOT NULL
            );""")


    return executeBatchQueries(sqlQueries)
}
