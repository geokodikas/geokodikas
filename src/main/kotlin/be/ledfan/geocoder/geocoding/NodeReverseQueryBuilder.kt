package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmNode
import java.sql.ResultSet

class NodeReverseQueryBuilder(humanAddressBuilderService: HumanAddressBuilderService) : ReverseQueryBuilder(humanAddressBuilderService) {

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
               layer,"""
        if (includeGeometry) {
            currentQuery += "centroid,"
        }
        currentQuery += """
               st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326), centroid) AS metric_distance
            FROM osm_node
        """

        withWhere("ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, centroid::geography, ?)")

        if (!hasLayerLimits) {
            withWhere("layer IN ('VirtualTrafficFlow'::Layer, 'PhysicalTrafficFlow'::Layer, 'Junction'::Layer)")
        }
    }

    override fun processResult(result: ResultSet): OsmEntity {
        val node = OsmNode.fillFromRow(result)
        return processEntity(node, result)
    }

}

