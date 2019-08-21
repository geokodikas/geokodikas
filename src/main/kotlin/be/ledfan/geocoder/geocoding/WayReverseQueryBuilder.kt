package be.ledfan.geocoder.geocoding

class WayReverseQueryBuilder(debug: Boolean = false) : ReverseQueryBuilder(debug) {

    override fun cteQuery(lon: Double, lat: Double): String {
        return """
            SELECT osm_id,
               version,
               tags,
               z_order,
               has_one_way_restriction,
               has_reversed_oneway,
               layer,
               geometry                                                                                       AS geometry,
               ST_distance(ST_SetSRID(ST_Point(4.409658908843995, 51.22282456687231), 4326), geometry)        AS distance,
               st_distance_sphere(ST_SetSRID(ST_Point(4.409658908843995, 51.22282456687231), 4326),
                                  geometry)                                                                   AS metric_distance,
               st_asbinary(st_closestpoint(geometry,
                                           ST_SetSRID(ST_Point(4.409658908843995, 51.22282456687231), 4326))) AS closest_point
            FROM osm_way
            WHERE ST_DWithin(ST_SetSRID(ST_Point(4.409658908843995, 51.22282456687231), 4326), geometry, 0.006)
            AND layer in ('VirtualTrafficFlow'::Layer, 'Junction'::Layer, 'Link'::Layer, 'Street'::Layer) 
        """.trimIndent()
    }

}

