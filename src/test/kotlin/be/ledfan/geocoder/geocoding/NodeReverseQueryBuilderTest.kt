package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.trimDup
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.contracts.ExperimentalContracts
import org.junit.jupiter.api.Assertions.assertEquals

@ExperimentalContracts
class NodeReverseQueryBuilderTest {

    private val humanAddressBuilderService = mockk<HumanAddressBuilderService>()

    @Test
    fun simple_test() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val nodeReverseQueryBuilder = NodeReverseQueryBuilder(humanAddressBuilderService)
        val req = ReverseGeocodeRequest.defaults.copy(lat = lat, lon = lon, limitRadius = radius )
        nodeReverseQueryBuilder.setupArgs(req)
        nodeReverseQueryBuilder.build()

        val query = nodeReverseQueryBuilder.currentQuery
        val parameters = nodeReverseQueryBuilder.parameters

        assertEquals("""SELECT 
                            osm_id, version, tags, z_order, layer, centroid,
                            st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326), centroid) AS metric_distance 
                        FROM osm_node 
                        WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, centroid::geography, ?) 
                        AND layer IN ('VirtualTrafficFlow'::Layer, 'PhysicalTrafficFlow'::Layer, 'Junction'::Layer)
                        ORDER BY metric_distance LIMIT ?""".trimMargin().trimDup(), query.trimDup())
        assertEquals(6, parameters.size)
        assertEquals(lon, parameters[0])
        assertEquals(lat, parameters[1])
        assertEquals(lon, parameters[2])
        assertEquals(lat, parameters[3])
        assertEquals(radius, parameters[4])
        assertEquals(ReverseGeocodeRequest.defaults.limitNumeric, 5)
    }

    @Test
    fun complex_test() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val limit = 13
        val layer = Layer.Venue
        val nodeReverseQueryBuilder = NodeReverseQueryBuilder(humanAddressBuilderService)

        val req = ReverseGeocodeRequest.defaults.copy(lat = lat, lon = lon, limitRadius = radius,
                limitNumeric = limit, limitLayers = listOf(layer), hasLayerLimits = true)
        nodeReverseQueryBuilder.setupArgs(req)
        nodeReverseQueryBuilder.build()

        val query = nodeReverseQueryBuilder.currentQuery
        val parameters = nodeReverseQueryBuilder.parameters

        assertEquals("""SELECT 
                            osm_id, version, tags, z_order, layer, centroid, st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326), centroid) AS metric_distance 
                            FROM osm_node WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, centroid::geography, ?) 
                        AND (layer = ?::Layer ) 
                        ORDER BY metric_distance 
                        LIMIT ?""".trimDup(), query.trimDup())

        assertEquals(7, parameters.size)
        assertEquals(lon, parameters[0])
        assertEquals(lat, parameters[1])
        assertEquals(lon, parameters[2])
        assertEquals(lat, parameters[3])
        assertEquals(radius, parameters[4])
        assertEquals(layer, Layer.valueOf(parameters[5] as String))
        assertEquals(limit, parameters[6])
    }


    @Test
    fun complex_test2() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val limit = 13
        val layer1 = Layer.Venue
        val layer2 = Layer.Address
        val nodeReverseQueryBuilder = NodeReverseQueryBuilder(humanAddressBuilderService)

        val req = ReverseGeocodeRequest.defaults.copy(lat = lat, lon = lon, limitRadius = radius,
                limitNumeric = limit, limitLayers = listOf(layer1, layer2), hasLayerLimits = true)
        nodeReverseQueryBuilder.setupArgs(req)
        nodeReverseQueryBuilder.build()

        val query = nodeReverseQueryBuilder.currentQuery
        val parameters = nodeReverseQueryBuilder.parameters

        assertEquals("""SELECT 
                            osm_id, version, tags, z_order, layer, centroid, st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326), centroid) AS metric_distance
                        FROM osm_node WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, centroid::geography, ?) 
                        AND (layer = ?::Layer OR layer = ?::Layer ) 
                        ORDER BY metric_distance LIMIT ?""".trimDup(), query.trimDup())

        assertEquals(8, parameters.size)
        assertEquals(lon, parameters[0])
        assertEquals(lat, parameters[1])
        assertEquals(lon, parameters[2])
        assertEquals(lat, parameters[3])
        assertEquals(radius, parameters[4])
        assertEquals(layer1, Layer.valueOf(parameters[5] as String))
        assertEquals(layer2, Layer.valueOf(parameters[6] as String))
        assertEquals(limit, parameters[7])
    }

}