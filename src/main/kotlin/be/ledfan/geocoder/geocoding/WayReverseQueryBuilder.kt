package be.ledfan.geocoder.geocoding

class WayReverseQueryBuilder(debug: Boolean = false) : ReverseQueryBuilder(debug) {

    override fun specificBaseQuery(lon: Double, lat: Double, metricDistance: Int) {
        repeat(2) {
            parameters.add(lon)
            parameters.add(lat)
        }
        parameters.add(metricDistance)
        currentQuery = """
            SELECT osm_id,
               version,
               tags,
               z_order,
               has_one_way_restriction,
               has_reversed_oneway,
               layer,
               geometry,
               centroid,
               st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326), geometry) AS metric_distance
            FROM osm_way
        """
        withWhere("ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, geometry::geography, ?)")
//        AND layer in ('VirtualTrafficFlow'::Layer, 'Junction'::Layer, 'Link'::Layer, 'Street'::Layer)
    }

}

