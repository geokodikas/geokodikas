package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmRelation
import java.sql.ResultSet

class RelationReverseQueryBuilder(humanAddressBuilderService: HumanAddressBuilderService) : ReverseQueryBuilder(humanAddressBuilderService) {

    override fun initQuery() {
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

    override fun processResult(result: ResultSet): OsmEntity {
        val node = OsmRelation.fillFromRow(result)
        return processEntity(node, result)
    }

}

