package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.addresses.LangCode
import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmEntity
import java.sql.ResultSet

class AddressIndexReverseQueryBuilder(humanAddressBuilderService: HumanAddressBuilderService) : ReverseQueryBuilder(humanAddressBuilderService) {

    override fun initQuery() {
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

    override fun processResult(result: ResultSet): OsmEntity {
        val addressIndex = AddressIndex.fillFromRow(result)
        val address = humanAddressBuilderService.build(LangCode.NL, addressIndex)
        addressIndex.dynamicProperties["Address"] = address
        return processEntity(addressIndex, result)
    }

}

