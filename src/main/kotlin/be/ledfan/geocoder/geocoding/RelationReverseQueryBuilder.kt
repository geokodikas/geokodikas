package be.ledfan.geocoder.geocoding

class RelationReverseQueryBuilder(debug: Boolean = false) : ReverseQueryBuilder(debug) {

    override fun specificBaseQuery(lon: Double, lat: Double, metricDistance: Int, hasLayerLimits: Boolean) {
        parameters.add(lon)
        parameters.add(lat)
        currentQuery = """
            SELECT osm_id,
                   version,
                   tags,
                   z_order,
                   layer,
                   geometry,
                   name,
                   0 as metric_distance
            FROM osm_relation
        """
        withWhere("ST_Within(ST_SetSRID(ST_Point(?, ?), 4326), geometry)")
        if (!hasLayerLimits) {
            withWhere("layer IN ('MacroRegion', 'LocalAdmin', 'County', 'Neighbourhood', 'Country')")
        }

    }

}

