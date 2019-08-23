package be.ledfan.geocoder.importer.steps

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.importer.BuildAddressIndex
import be.ledfan.geocoder.kodein
import org.intellij.lang.annotations.Language
import org.kodein.di.direct
import org.kodein.di.generic.instance

suspend fun step4_checks(): Boolean {
    return ensureNotEmpty("osm_way")
            && ensureNotEmpty("osm_node")
            && ensureNotEmpty("osm_relation")
            && ensureNotEmpty("parent")
}

suspend fun step4_materialized_view(): Boolean {
    @Language("SQL")
    val sqlQuerries = listOf(
            """
                CREATE MATERIALIZED VIEW IF NOT EXISTS streets
                AS
                SELECT osm_way.osm_id                          AS way_id,
                       regexp_replace(unaccent(lower(osm_way.tags -> 'name')), '[\s[:punct:]]', '', 'g')
                        AS street_name,
                       parent.parent_id                        AS localadmin_id,
                       osm_way.geometry                        AS geometry
                FROM osm_way
                   JOIN parent ON parent.child_id = osm_way.osm_id AND parent.parent_layer = 'LocalAdmin'::Layer
                    WHERE osm_way.layer = 'Street'::Layer
                         AND osm_way.tags -> 'name' IS NOT NULL
            """,
            """CREATE INDEX IF NOT EXISTS streets_idx ON streets (localadmin_id, street_name) """,
            """CREATE INDEX IF NOT EXISTS streets_idx ON streets (localadmin_id)"""
    )

    return executeBatchQueries(sqlQuerries)
}

suspend fun step4_build_address_index(): Boolean {

    val buildAddressIndex: BuildAddressIndex = kodein.direct.instance()
    buildAddressIndex.build()

    return true
}
