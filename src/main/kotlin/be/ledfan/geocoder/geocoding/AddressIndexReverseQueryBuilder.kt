package be.ledfan.geocoder.geocoding

class AddressIndexReverseQueryBuilder(debug: Boolean = false) : ReverseQueryBuilder(debug) {

    override fun cteQuery(lon: Double, lat: Double): String {
        repeat(4) {
            parameters.add(lon)
            parameters.add(lat)
        }
        // TODO tags?
        return """
            SELECT 
               tags,
               geometry,
               centroid
            FROM address_index
            WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326), geometry, 0.006)
        """
    }

}

