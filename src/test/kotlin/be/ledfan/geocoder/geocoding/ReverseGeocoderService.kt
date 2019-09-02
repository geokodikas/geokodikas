package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.db.entity.*
import be.ledfan.geocoder.importer.Layer
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.locationtech.jts.geom.Coordinate
import org.postgis.PGgeometry
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals

@ExperimentalContracts
class ReverseGeocoderServiceTest {

    data class Mocks(val reverseGeocoderService: ReverseGeocoderService,
                     val nodeReverseQueryBuilder: NodeReverseQueryBuilder,
                     val wayReverseQueryBuilder: WayReverseQueryBuilder,
                     val addressIndexReverseQueryBuilder: AddressIndexReverseQueryBuilder,
                     val relationReverseQueryBuilder: RelationReverseQueryBuilder)

    private fun setupMocks(): Mocks {
        val humanAddressBuilderService = mockk<HumanAddressBuilderService>()
        val reverseQueryBuilderFactory = mockk<ReverseQueryBuilderFactory>()
        val osmType = slot<OsmType>()
        val hm = slot<HumanAddressBuilderService>()

        val mocks = Mocks(ReverseGeocoderService(reverseQueryBuilderFactory,
                humanAddressBuilderService), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        val entities = slot<MutableList<OsmEntity>>()
        every { mocks.nodeReverseQueryBuilder.execute(capture(entities)) } answers {
            entities.captured.run {
                add(OsmNode.create(100, Layer.Junction).also {
                    it.dynamicProperties["distance"] = 5
                    it.centroid = PGgeometry(PGgeometry.geomFromString("POINT(1.40 50.50)"))
                })
                add(OsmNode.create(101, Layer.Junction).also {
                    it.dynamicProperties["distance"] = 86
                    it.centroid = PGgeometry(PGgeometry.geomFromString("POINT(2.40 50.50)"))
                })
                add(OsmNode.create(102, Layer.Junction).also {
                    it.dynamicProperties["distance"] = 17
                    it.centroid = PGgeometry(PGgeometry.geomFromString("POINT(3.40 50.50)"))
                })
            }
        }

        every { mocks.wayReverseQueryBuilder.execute(capture(entities)) } answers {
            entities.captured.run {
                add(OsmWay.create(200, Layer.Street).also {
                    it.dynamicProperties["distance"] = 42
                    it.geometry = PGgeometry(PGgeometry.geomFromString("POINT(4.40 50.50)"))
                })
                add(OsmWay.create(201, Layer.Link).also {
                    it.dynamicProperties["distance"] = 12
                    it.geometry = PGgeometry(PGgeometry.geomFromString("POINT(5.40 50.50)"))
                })
                add(OsmWay.create(202, Layer.Street).also {
                    it.dynamicProperties["distance"] = 23
                    it.geometry = PGgeometry(PGgeometry.geomFromString("POINT(6.40 50.50)"))
                })
            }
        }

        every { mocks.relationReverseQueryBuilder.execute(capture(entities)) } answers {
            entities.captured.run {
                add(OsmRelation.create(300, "localadmin", Layer.LocalAdmin).also {
                    it.dynamicProperties["distance"] = 9
                    it.geometry = PGgeometry(PGgeometry.geomFromString("POINT(7.40 50.50)"))
                })
                add(OsmRelation.create(301, "neighbourhood", Layer.Neighbourhood).also {
                    it.dynamicProperties["distance"] = 3
                    it.geometry = PGgeometry(PGgeometry.geomFromString("POINT(8.40 50.50)"))
                })
                add(OsmRelation.create(302, "localadmin2", Layer.LocalAdmin).also {
                    it.dynamicProperties["distance"] = 19
                    it.geometry = PGgeometry(PGgeometry.geomFromString("POINT(9.40 50.50)"))
                })
            }
        }
        every { mocks.addressIndexReverseQueryBuilder.execute(capture(entities)) } answers {
            entities.captured.run {
                add(AddressIndex.create(400, OsmType.Node).also {
                    it.dynamicProperties["distance"] = 48
                    it.geometry = PGgeometry(PGgeometry.geomFromString("POINT(10.40 50.50)"))
                })
                add(AddressIndex.create(401, OsmType.Way).also {
                    it.dynamicProperties["distance"] = 72
                    it.geometry = PGgeometry(PGgeometry.geomFromString("POINT(11.40 50.50)"))
                })
                add(AddressIndex.create(402, OsmType.Node).also {
                    it.dynamicProperties["distance"] = 45
                    it.geometry = PGgeometry(PGgeometry.geomFromString("POINT(12.40 50.50)"))
                })
            }
        }

        every { reverseQueryBuilderFactory.createBuilder(capture(osmType), capture(hm)) } answers {
            val givenType: OsmType = firstArg()
            when (givenType) {
                OsmType.Node -> {
                    mocks.nodeReverseQueryBuilder
                }
                OsmType.Way -> {
                    mocks.wayReverseQueryBuilder
                }
                OsmType.Relation -> {
                    mocks.relationReverseQueryBuilder
                }
                OsmType.AddressIndex -> {
                    mocks.addressIndexReverseQueryBuilder
                }
            }
        }

        return mocks
    }

    @Test
    fun test_default_parameters() {
        val mocks = setupMocks()
        val res = runBlocking {
            mocks.reverseGeocoderService.reverseGeocode(10.42, 12.42, limitNumeric = null, limitRadius = null, desiredLayers = null)
        }

        verify(exactly = 0) { mocks.nodeReverseQueryBuilder.execute(any()) }
        verify(exactly = 0) { mocks.relationReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.wayReverseQueryBuilder.setupArgs(10.42, 12.42, 200, false) }
        verify(exactly = 1) { mocks.wayReverseQueryBuilder.limit(5) }
        verify(exactly = 0) { mocks.wayReverseQueryBuilder.whereLayer(any()) }
        verify(exactly = 1) { mocks.wayReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.setupArgs(10.42, 12.42, 200, false) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.limit(5) }
        verify(exactly = 0) { mocks.addressIndexReverseQueryBuilder.whereLayer(any()) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.execute(any()) }

        assertEquals(Coordinate(5.4, 50.5), res.closestPoint)
        assertEquals(5, res.entities.size) // no limit provided, default limit should be 5
        assertEquals(listOf<Long>(201, 202, 200, 402, 400), res.entities.map { it.id })
        assertEquals(listOf<Long>(201, 202, 200, 402, 400), res.order)
    }

    @Test
    fun `test when limit is lower than the number of results returned by single table`() {

        val mocks = setupMocks()
        val res = runBlocking {
            mocks.reverseGeocoderService.reverseGeocode(10.42, 12.42, limitNumeric = 1, limitRadius = null, desiredLayers = null)
        }

        verify(exactly = 0) { mocks.nodeReverseQueryBuilder.execute(any()) }
        verify(exactly = 0) { mocks.relationReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.wayReverseQueryBuilder.setupArgs(10.42, 12.42, 200, false) }
        verify(exactly = 1) { mocks.wayReverseQueryBuilder.limit(1) }
        verify(exactly = 0) { mocks.wayReverseQueryBuilder.whereLayer(any()) }
        verify(exactly = 1) { mocks.wayReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.setupArgs(10.42, 12.42, 200, false) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.limit(1) }
        verify(exactly = 0) { mocks.addressIndexReverseQueryBuilder.whereLayer(any()) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.execute(any()) }

        assertEquals(Coordinate(5.4, 50.5), res.closestPoint)
        assertEquals(1, res.entities.size) // no limit provided, default limit should be 5
        assertEquals(listOf<Long>(201), res.entities.map { it.id })
        assertEquals(listOf<Long>(201), res.order)
    }

    @Test
    fun test_default_layers() {
        // default layers are Layer.Address, Layer.Venue, Layer.Street, Layer.Link
        // thus should only search in AddressIndex and Way
        val mocks = setupMocks()
        val res = runBlocking {
            mocks.reverseGeocoderService.reverseGeocode(10.42, 12.42, limitNumeric = 100, limitRadius = null, desiredLayers = null)
        }

        verify(exactly = 0) { mocks.nodeReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.wayReverseQueryBuilder.execute(any()) }
        verify(exactly = 0) { mocks.relationReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.execute(any()) }

        assertEquals(Coordinate(5.4, 50.5), res.closestPoint)
        assertEquals(6, res.entities.size) // no limit provided, default limit should be 5
        assertEquals(listOf<Long>(201, 202, 200, 402, 400, 401), res.entities.map { it.id })
        assertEquals(listOf<Long>(201, 202, 200, 402, 400, 401), res.order)
    }

    @Test
    fun test_all_layers() {
        // default layers are Layer.Address, Layer.Venue, Layer.Street, Layer.Link
        // thus should only search in AddressIndex and Way
        val mocks = setupMocks()
        val res = runBlocking {
            mocks.reverseGeocoderService.reverseGeocode(10.42, 12.42, limitNumeric = 100, limitRadius = null, desiredLayers = Layer.values().toList())
        }

        verify(exactly = 1) { mocks.nodeReverseQueryBuilder.limit(100) }
        verify(exactly = 1) { mocks.nodeReverseQueryBuilder.setupArgs(10.42, 12.42, 200, true) }
        verify(exactly = 1) { mocks.nodeReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.wayReverseQueryBuilder.setupArgs(10.42, 12.42, 200, true) }
        verify(exactly = 1) { mocks.wayReverseQueryBuilder.limit(100) }
        verify(exactly = 1) { mocks.wayReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.relationReverseQueryBuilder.setupArgs(10.42, 12.42, 200, true) }
        verify(exactly = 1) { mocks.relationReverseQueryBuilder.limit(100) }
        verify(exactly = 1) { mocks.relationReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.setupArgs(10.42, 12.42, 200, true) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.limit(100) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.execute(any()) }

        assertEquals(Coordinate(8.4, 50.5), res.closestPoint)
        assertEquals(12, res.entities.size) // no limit provided, default limit should be 5
        assertEquals(listOf<Long>(301, 100, 300, 201, 102, 302, 202, 200, 402, 400, 401, 101), res.entities.map { it.id })
        assertEquals(listOf<Long>(301, 100, 300, 201, 102, 302, 202, 200, 402, 400, 401, 101), res.order)
    }

    @Test
    fun test_specific_layers() {
        // default layers are Layer.Address, Layer.Venue, Layer.Street, Layer.Link
        // thus should only search in AddressIndex and Way
        val mocks = setupMocks()
        val res = runBlocking {
            mocks.reverseGeocoderService.reverseGeocode(10.42, 12.42, limitNumeric = 100, limitRadius = null, desiredLayers = listOf(Layer.Venue, Layer.LocalAdmin))
        }

        verify(exactly = 0) { mocks.nodeReverseQueryBuilder.execute(any()) }
        verify(exactly = 0) { mocks.wayReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.relationReverseQueryBuilder.setupArgs(10.42, 12.42, 200, true) }
        verify(exactly = 1) { mocks.relationReverseQueryBuilder.limit(100) }
        verify(exactly = 1) { mocks.relationReverseQueryBuilder.whereLayer(listOf(Layer.Venue, Layer.LocalAdmin)) }
        verify(exactly = 1) { mocks.relationReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.setupArgs(10.42, 12.42, 200, true) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.limit(100) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.execute(any()) }
        verify(exactly = 1) { mocks.addressIndexReverseQueryBuilder.whereLayer(listOf(Layer.Venue, Layer.LocalAdmin)) }

        assertEquals(Coordinate(8.4, 50.5), res.closestPoint)
        assertEquals(6, res.entities.size) // no limit provided, default limit should be 5
        assertEquals(listOf<Long>(301, 300, 302, 402, 400, 401), res.entities.map { it.id })
        assertEquals(listOf<Long>(301, 300, 302, 402, 400, 401), res.order)
    }

}