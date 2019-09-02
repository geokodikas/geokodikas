package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.trimDup
import io.mockk.mockk
import org.junit.Test
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals

@ExperimentalContracts
class AddressIndexReverseQueryBuilderTest {

    private val humanAddressBuilderService = mockk<HumanAddressBuilderService>()

    @Test
    fun simple_test() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val addRelationReverseQueryBuilder = AddressIndexReverseQueryBuilder(humanAddressBuilderService)
        addRelationReverseQueryBuilder.setupArgs(lat, lon, radius, false)
        addRelationReverseQueryBuilder.initQuery()

        val query = addRelationReverseQueryBuilder.currentQuery
        val parameters = addRelationReverseQueryBuilder.parameters

        assertEquals("""SELECT osm_id,
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
                        WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, geometry::geography, ?)""".trimDup(), query.trimDup())
        assertEquals(5, parameters.size)
        assertEquals(lon, parameters[0])
        assertEquals(lat, parameters[1])
        assertEquals(lon, parameters[2])
        assertEquals(lat, parameters[3])
        assertEquals(radius, parameters[4])
    }

    @Test
    fun complex_test() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val limit = 13
        val layer = Layer.Venue
        val addRelationReverseQueryBuilder = AddressIndexReverseQueryBuilder(humanAddressBuilderService)

        addRelationReverseQueryBuilder.setupArgs(lat, lon, radius, true)
        addRelationReverseQueryBuilder.initQuery()
        addRelationReverseQueryBuilder.whereLayer(listOf(layer))
        addRelationReverseQueryBuilder.orderBy()
        addRelationReverseQueryBuilder.limit(limit)

        val query = addRelationReverseQueryBuilder.currentQuery
        val parameters = addRelationReverseQueryBuilder.parameters

        assertEquals("""SELECT osm_id,
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
                        WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, geometry::geography, ?)
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
        val addRelationReverseQueryBuilder = AddressIndexReverseQueryBuilder(humanAddressBuilderService)

        addRelationReverseQueryBuilder.setupArgs(lat, lon, radius, true)
        addRelationReverseQueryBuilder.initQuery()
        addRelationReverseQueryBuilder.whereLayer(listOf(layer1, layer2))
        addRelationReverseQueryBuilder.orderBy()
        addRelationReverseQueryBuilder.limit(limit)

        val query = addRelationReverseQueryBuilder.currentQuery
        val parameters = addRelationReverseQueryBuilder.parameters

        assertEquals("""SELECT osm_id,
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
                        WHERE ST_DWithin(ST_SetSRID(ST_Point(?, ?), 4326)::geography, geometry::geography, ?)
                        AND (layer = ?::Layer OR layer = ?::Layer )
                        ORDER BY metric_distance 
                        LIMIT ?""".trimDup(), query.trimDup())

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

    @Test
    fun test_debug() {
        val lat = 10.42
        val lon = 12.42
        val radius = 100
        val addRelationReverseQueryBuilder = AddressIndexReverseQueryBuilder(humanAddressBuilderService)
        addRelationReverseQueryBuilder.setupArgs(lat, lon, radius, false)
        addRelationReverseQueryBuilder.initQuery()

        val query = addRelationReverseQueryBuilder.buildQueryForDebugging()
        val parameters = addRelationReverseQueryBuilder.parameters

        assertEquals("""SELECT osm_id,
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
                           st_distance_sphere(ST_SetSRID(ST_Point($lon, $lat), 4326), geometry) AS metric_distance
                        FROM address_index
                        WHERE ST_DWithin(ST_SetSRID(ST_Point($lon, $lat), 4326)::geography, geometry::geography, $radius)""".trimDup(), query.trimDup())
    }

}