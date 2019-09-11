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
WITH tmp_data AS
     ((SELECT 'w'              AS relation_type,
              osm_way.osm_id   AS osm_id,
              osm_way.tags     AS tags,
              osm_way.geometry AS geometry,
              parent_id        AS localadmin_id
       FROM osm_way
                JOIN parent ON parent.child_id = osm_way.osm_id AND parent.parent_layer = 'LocalAdmin'::Layer
       WHERE osm_way.layer = 'Street'::Layer
         AND osm_way.tags -> 'name' IS NOT NULL)
      UNION
      (SELECT 'r'                   AS relation_type,
              osm_relation.osm_id   AS osm_id,
              osm_relation.tags     AS tags,
              osm_relation.geometry AS geometry,
              parent_id             AS localadmin_id
       FROM osm_relation
                JOIN parent ON parent.child_id = osm_relation.osm_id AND parent.parent_layer = 'LocalAdmin'::Layer
       WHERE osm_relation.layer = 'Venue'::Layer
         AND osm_relation.tags -> 'name' IS NOT NULL))

SELECT * FROM (
                  SELECT tmp_data.relation_type,
                         tmp_data.osm_id,
                         tmp_data.geometry,
                         tmp_data.localadmin_id,
                         regexp_replace(
                                 unaccent(lower(unnest(regexp_split_to_array(tmp_data.tags -> tag_name, E'[\,/-]')))),
                                 '[\s[:punct:]]', '',
                                 'g') AS street_name
                  FROM tmp_data,
                       (SELECT unnest(
                                       ARRAY ['name', 'name:nl', 'name:fr', 'alt_name','alt_name:nl', 'alt_name:fr', 'name:left', 'name:right']) AS tag_name) AS tn
                  UNION
                  SELECT tmp_data.relation_type,
                         tmp_data.osm_id,
                         tmp_data.geometry,
                         tmp_data.localadmin_id,
                         regexp_replace(unaccent(lower(tmp_data.tags -> tag_name)), '[\s[:punct:]]', '', 'g') AS b
                  FROM tmp_data,
                       (SELECT unnest(ARRAY ['name', 'name:nl', 'name:fr', 'alt_name','alt_name:nl', 'alt_name:fr', 'name:left', 'name:right']) AS tag_name) AS tn
              ) as t where street_name is not null""",
            """CREATE INDEX IF NOT EXISTS streets_localadmin_streetname_idx ON streets (localadmin_id, street_name)""",
            """CREATE INDEX IF NOT EXISTS streets_streetname_idx ON streets (street_name)""",
            """CREATE INDEX IF NOT EXISTS streets_localadmin_idx ON streets (localadmin_id)""",
            """CREATE INDEX IF NOT EXISTS streets_localadmin_geometry_idx ON streets USING GIST(geometry)"""
    )

    return executeBatchQueries(sqlQuerries)
}

suspend fun step4_build_address_index(): Boolean {

    val buildAddressIndex: BuildAddressIndex = kodein.direct.instance()
    buildAddressIndex.build()

    return true
}

suspend fun step4_create_indexes(): Boolean {
    @Language("SQL")
    val sqlQuerries = listOf(
        """CREATE INDEX address_index_geom_idx ON address_index USING gist (geometry)""",
        """CREATE INDEX address_index_geom_geography_idx ON address_index USING gist (geography(geometry))""")

    return executeBatchQueriesParallel(sqlQuerries)
}
