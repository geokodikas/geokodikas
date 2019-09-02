package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.trimDup
import io.mockk.mockk
import org.junit.Test
import java.lang.Exception
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows

@ExperimentalContracts
class NodeReverseQueryBuilderTest {

    private val humanAddressBuilderService = mockk<HumanAddressBuilderService>()

    @Test
    fun simple_test() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val nodeReverseQueryBuilder = NodeReverseQueryBuilder(humanAddressBuilderService)
        nodeReverseQueryBuilder.setupArgs(lat, lon, radius, false)
        nodeReverseQueryBuilder.initQuery()

        val query = nodeReverseQueryBuilder.currentQuery
        val parameters = nodeReverseQueryBuilder.parameters

        assertEquals("""SELECT 
                            osm_id, version, tags, z_order, layer, centroid,
                            st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326), centroid) AS metric_distance 
                        FROM osm_node 
                        WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, centroid::geography, ?) 
                        AND layer IN ('VirtualTrafficFlow'::Layer, 'PhysicalTrafficFlow'::Layer, 'Junction'::Layer)""".trimMargin().trimDup(), query.trimDup())
        assertEquals(5, parameters.size)
        assertEquals(lon, parameters[0])
        assertEquals(lat, parameters[1])
        assertEquals(lon, parameters[2])
        assertEquals(lat, parameters[3])
        assertEquals(radius, parameters[4])
    }

    @Test
    fun simple_test2() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val nodeReverseQueryBuilder = NodeReverseQueryBuilder(humanAddressBuilderService)

        nodeReverseQueryBuilder.setupArgs(lat, lon, radius, false)
        nodeReverseQueryBuilder.initQuery()
        val exception = assertThrows(Exception::class.java) {
            nodeReverseQueryBuilder.whereLayer(listOf(Layer.Address))
        }
        assertEquals("HasLayerLimits is false, cannot specify layer to limit on", exception.message)

//        val (query, parameters) = nodeReverseQueryBuilder.build()
//
//        assertEquals("""SELECT
//                            osm_id, version, tags, z_order, layer, centroid, st_distance_sphere(ST_SetSRID(ST_Point(?, ?), 4326), centroid) AS metric_distance
//                        FROM osm_node
//                        WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, centroid::geography, ?)""".trimMargin().trimDup(), query.trimDup())
//        assertEquals(5, parameters.size)
//        assertEquals(lon, parameters[0])
//        assertEquals(lat, parameters[1])
//        assertEquals(lon, parameters[2])
//        assertEquals(lat, parameters[3])
//        assertEquals(radius, parameters[4])
    }

    @Test
    fun complex_test() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val limit = 13
        val layer = Layer.Venue
        val nodeReverseQueryBuilder = NodeReverseQueryBuilder(humanAddressBuilderService)

        nodeReverseQueryBuilder.setupArgs(lat, lon, radius, true)
        nodeReverseQueryBuilder.initQuery()
        nodeReverseQueryBuilder.whereLayer(listOf(layer))
        nodeReverseQueryBuilder.orderBy()
        nodeReverseQueryBuilder.limit(limit)

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

        nodeReverseQueryBuilder.setupArgs(lat, lon, radius, true)
        nodeReverseQueryBuilder.initQuery()
        nodeReverseQueryBuilder.whereLayer(listOf(layer1, layer2))
        nodeReverseQueryBuilder.orderBy()
        nodeReverseQueryBuilder.limit(limit)

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