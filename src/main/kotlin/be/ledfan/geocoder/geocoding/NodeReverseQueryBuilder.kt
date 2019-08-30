package be.ledfan.geocoder.geocoding

class NodeReverseQueryBuilder(debug: Boolean = false) : ReverseQueryBuilder(debug) {

    override fun specificBaseQuery(lon: Double, lat: Double, metricDistance: Int, hasLayerLimits: Boolean) {
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

        withWhere("ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, centroid::geography, ?)")

        if (!hasLayerLimits) {
            withWhere("layer IN ('VirtualTrafficFlow'::Layer, 'PhysicalTrafficFlow'::Layer, 'Junction'::Layer)")
        }
    }

}

