package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.trimDup
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.contracts.ExperimentalContracts
import org.junit.jupiter.api.Assertions.assertEquals

@ExperimentalContracts
class RelationReverseQueryBuilderTest {

    private val humanAddressBuilderService = mockk<HumanAddressBuilderService>()

    @Test
    fun simple_test() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val relationReverseQueryBuilder = RelationReverseQueryBuilder(humanAddressBuilderService)

        val req = ReverseGeocodeRequest.defaults.copy(lat = lat, lon = lon, limitRadius = radius)
        relationReverseQueryBuilder.setupArgs(req)
        relationReverseQueryBuilder.build()

        val query = relationReverseQueryBuilder.currentQuery
        val parameters = relationReverseQueryBuilder.parameters

        assertEquals("""SELECT
                            osm_id, version, tags, z_order, layer, geometry, name, 0 as metric_distance 
                        FROM osm_relation WHERE ST_Within(ST_SetSRID(ST_Point(?, ?), 4326), geometry) 
                        AND layer IN ('MacroRegion', 'LocalAdmin', 'County', 'Neighbourhood', 'Country')
                        ORDER BY metric_distance LIMIT ?""".trimDup(), query.trimDup())
        assertEquals(3, parameters.size)
        assertEquals(lon, parameters[0])
        assertEquals(lat, parameters[1])
        assertEquals(ReverseGeocodeRequest.defaults.limitNumeric, 5)
    }

    @Test
    fun complex_test() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val limit = 13
        val layer = Layer.Venue
        val relationReverseQueryBuilder = RelationReverseQueryBuilder(humanAddressBuilderService)

        val req = ReverseGeocodeRequest.defaults.copy(lat = lat, lon = lon,  limitNumeric = limit,
                limitRadius = radius, limitLayers = listOf(layer), hasLayerLimits = true)
        relationReverseQueryBuilder.setupArgs(req)
        relationReverseQueryBuilder.build()

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