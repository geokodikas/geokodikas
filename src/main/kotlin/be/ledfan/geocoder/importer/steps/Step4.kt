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
            """CREATE MATERIALIZED VIEW IF NOT EXISTS streets
AS
SELECT DISTINCT relation_type,
                osm_id,
                ARRAY((SELECT DISTINCT unnest(array_agg(DISTINCT street_names.a) ||
                                              array_agg(DISTINCT street_names.b))
                       FROM (SELECT regexp_replace(
                                            unaccent(lower(
                                                    unnest(regexp_split_to_array(tags -> tag_name, E'[\,/-]')))),
                                            '[\s[:punct:]]', '', 'g') AS a,
                                    regexp_replace(unaccent(lower(tags -> tag_name)), '[\s[:punct:]]', '',
                                                   'g')               AS b
                             FROM (SELECT unnest(
                                                  ARRAY ['name', 'name:nl', 'name:fr', 'alt_name','alt_name:nl', 'alt_name:fr', 'name:left', 'name:right']) AS tag_name) AS tn)
                                AS street_names)) AS street_name,
                localadmin_id,
                geometry                          AS geometry
FROM ((SELECT 'w'       AS relation_type,osm_way.osm_id as osm_id,
             osm_way.tags as tags,
             osm_way.geometry as geometry,
             parent_id AS localadmin_id
      FROM osm_way
               JOIN parent ON parent.child_id = osm_way.osm_id AND parent.parent_layer = 'LocalAdmin'::Layer
      WHERE osm_way.layer = 'Street'::Layer
        AND osm_way.tags -> 'name' IS NOT NULL)
UNION
(SELECT 'r'       AS relation_type,
        osm_relation.osm_id as osm_id,
        osm_relation.tags as tags,
        osm_relation.geometry as geometry,
        parent_id AS localadmin_id
 FROM osm_relation
          JOIN parent ON parent.child_id = osm_relation.osm_id AND parent.parent_layer = 'LocalAdmin'::Layer
 WHERE osm_relation.layer = 'Venue'::Layer
   AND osm_relation.tags -> 'name' IS NOT NULL)) as owporp""",
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
