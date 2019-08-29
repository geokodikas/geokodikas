package be.ledfan.geocoder.geocoding

class NodeReverseQueryBuilder(debug: Boolean = false) : ReverseQueryBuilder(debug) {

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
               layer,
               centroid                                                                                       ,
               ST_distance(ST_SetSRID(ST_Point(?, ?), 4326), centroid)        AS distance,
               st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326),
                                  centroid)                                                                   AS metric_distance
            FROM osm_node
            WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326), centroid, 0.006)
            AND (layer = 'VirtualTrafficFlow'::Layer
             OR  layer = 'Junction'::Layer
             OR  layer = 'PhysicalTrafficFlow'::Layer)
        """.trimIndent()
    }

}

