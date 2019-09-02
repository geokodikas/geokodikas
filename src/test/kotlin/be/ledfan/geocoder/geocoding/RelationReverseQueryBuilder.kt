package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.trimDup
import io.mockk.mockk
import org.junit.Test
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals

@ExperimentalContracts
class RelationReverseQueryBuilderTEst {

    private val humanAddressBuilderService = mockk<HumanAddressBuilderService>()

    @Test
    fun simple_test() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val relationReverseQueryBuilder = RelationReverseQueryBuilder(humanAddressBuilderService)
        relationReverseQueryBuilder.setupArgs(lat, lon, radius, false)
        relationReverseQueryBuilder.initQuery()

        val query = relationReverseQueryBuilder.currentQuery
        val parameters = relationReverseQueryBuilder.parameters

        assertEquals("""SELECT
                            osm_id, version, tags, z_order, layer, geometry, name, 0 as metric_distance 
                        FROM osm_relation WHERE ST_Within(ST_SetSRID(ST_Point(?, ?), 4326), geometry) 
                        AND layer IN ('MacroRegion', 'LocalAdmin', 'County', 'Neighbourhood', 'Country')""".trimDup(), query.trimDup())
        assertEquals(2, parameters.size)
        assertEquals(lon, parameters[0])
        assertEquals(lat, parameters[1])
    }

    @Test
    fun complex_test() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val limit = 13
        val layer = Layer.Venue
        val relationReverseQueryBuilder = RelationReverseQueryBuilder(humanAddressBuilderService)

        relationReverseQueryBuilder.setupArgs(lat, lon, radius, true)
        relationReverseQueryBuilder.initQuery()
        relationReverseQueryBuilder.whereLayer(listOf(layer))
        relationReverseQueryBuilder.orderBy()
        relationReverseQueryBuilder.limit(limit)

        val query = relationReverseQueryBuilder.currentQuery
        val parameters = relationReverseQueryBuilder.parameters

        assertEquals("""SELECT 
                            osm_id, version, tags, z_order, layer, geometry, name, 0 as metric_distance FROM osm_relation 
                        WHERE ST_Within(ST_SetSRID(ST_Point(?, ?), 4326), geometry) AND (layer = ?::Layer ) 
                        ORDER BY metric_distance 
                        LIMIT ?""".trimDup(), query.trimDup())

        assertEquals(4, parameters.size)
        assertEquals(lon, parameters[0])
        assertEquals(lat, parameters[1])
        assertEquals(layer, Layer.valueOf(parameters[2] as String))
        assertEquals(limit, parameters[3])
    }

}