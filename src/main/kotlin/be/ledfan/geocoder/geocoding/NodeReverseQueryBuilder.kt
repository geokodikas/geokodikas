package be.ledfan.geocoder.geocoding

class NodeReverseQueryBuilder(debug: Boolean = false) : ReverseQueryBuilder(debug) {

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
               layer,
               centroid,
               st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326), centroid) AS metric_distance
            FROM osm_node
        """
//        AND (layer = 'VirtualTrafficFlow'::Layer
//                OR  layer = 'Junction'::Layer
//                OR  layer = 'PhysicalTrafficFlow'::Layer)
        withWhere("ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, centroid::geography, ?)")
    }

}

