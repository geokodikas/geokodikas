package be.ledfan.geocoder.geocoding

class AddressIndexReverseQueryBuilder(debug: Boolean = false) : ReverseQueryBuilder(debug) {

    override fun cteQuery(lon: Double, lat: Double): String {
        repeat(3) {
            parameters.add(lon)
            parameters.add(lat)
        }
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
                       layer,
                       geometry                                                                                       AS geometry,
                       ST_distance(ST_SetSRID(ST_Point(?, ?), 4326),
                                   geometry)                                                                          AS distance,
                       st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326),
                                          geometry)                                                                   AS metric_distance
                FROM address_index
                WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326), geometry, 0.006)
                """
    }

}

