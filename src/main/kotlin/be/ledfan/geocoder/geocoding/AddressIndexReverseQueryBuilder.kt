package be.ledfan.geocoder.geocoding

class AddressIndexReverseQueryBuilder(debug: Boolean = false) : ReverseQueryBuilder(debug) {

    override fun cteQuery(lon: Double, lat: Double): String {
        repeat(4) {
            parameters.add(lon)
            parameters.add(lat)
        }
        // TODO tags?
        return """
                SELECT osm_id,
                       tags,
                       osm_type,
                       street_id,
                       neighbourhood_id,
                       localadmin_id,
                       county_id,
                       macroregion_id,
                       country_id,
                       housenumber,
                       geometry                                                                                       AS geometry,
                       ST_distance(ST_SetSRID(ST_Point(?, ?), 4326),
                                   geometry)                                                                          AS distance,
                       st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326),
                                          geometry)                                                                   AS metric_distance,
                       st_asbinary(st_closestpoint(geometry,
                                                   ST_SetSRID(ST_Point(?, ?), 4326))) AS closest_point
                FROM address_index
                WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326), geometry, 0.006)
                """
    }

}

