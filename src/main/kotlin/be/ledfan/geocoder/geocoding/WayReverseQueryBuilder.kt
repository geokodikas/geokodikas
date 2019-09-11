package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmWay
import java.sql.ResultSet

class WayReverseQueryBuilder(humanAddressBuilderService: HumanAddressBuilderService) : ReverseQueryBuilder(humanAddressBuilderService) {

    override fun initQuery() {
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
               layer, """

        if (includeGeometry) {
            currentQuery += "geometry,"
        }
        currentQuery += """
               st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326), geometry) AS metric_distance
            FROM osm_way
            """
        withWhere("ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, geometry::geography, ?)")

        if (!hasLayerLimits) {
            withWhere("layer IN ('VirtualTrafficFlow'::Layer, 'PhysicalTrafficFlow'::Layer, 'Link'::Layer, 'Street'::Layer, 'Junction'::Layer)")
        }
    }

    override fun processResult(result: ResultSet): OsmEntity {
        val node = OsmWay.fillFromRow(result)
        return processEntity(node, result)
    }

}

