package be.ledfan.geocoder.geocoding

class WayReverseQueryBuilder(debug: Boolean = false) : ReverseQueryBuilder(debug) {

    override fun cteQuery(lon: Double, lat: Double): String {
        repeat(3) {
            parameters.add(lon)
            parameters.add(lat)
        }
        return """
            SELECT osm_id,
               version,
               tags,
               z_order,
               has_one_way_restriction,
               has_reversed_oneway,
               layer,
               geometry                                                                                       AS geometry,
               centroid,
               ST_distance(ST_SetSRID(ST_Point(?, ?), 4326), geometry)        AS distance,
               st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326),
                                  geometry)                                                                   AS metric_distance
            FROM osm_way
            WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326), geometry, 0.006)
            AND layer in ('VirtualTrafficFlow'::Layer, 'Junction'::Layer, 'Link'::Layer, 'Street'::Layer) 
        """
    }

}

