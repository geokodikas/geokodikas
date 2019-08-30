package be.ledfan.geocoder.geocoding

class AddressIndexReverseQueryBuilder(debug: Boolean = false) : ReverseQueryBuilder(debug) {

    override fun specificBaseQuery(lon: Double, lat: Double, metricDistance: Int, hasLayerLimits: Boolean) {
        repeat(2) {
            parameters.add(lon)
            parameters.add(lat)
        }
        parameters.add(metricDistance)
        currentQuery =  """
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
                       geometry                                                       AS geometry,
                       st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326), geometry) AS metric_distance
                FROM address_index
                """

        withWhere("ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, geometry::geography, ?)")
    }

}

