package importer.processors

import be.ledfan.geocoder.db.entity.OsmUpstreamElement
import be.ledfan.geocoder.db.mapper.OsmUpstreamLineMapper
import be.ledfan.geocoder.db.mapper.OsmUpstreamPolygonMapper
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import be.ledfan.geocoder.db.mapper.WayNodeMapper
import be.ledfan.geocoder.importer.DetermineLayerWay
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.importer.core.Tags
import be.ledfan.geocoder.importer.processors.OsmWayProcessor
import com.slimjars.dist.gnu.trove.list.array.TLongArrayList
import de.topobyte.osm4j.core.model.impl.Way
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.postgis.PGgeometry
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OsmWayProcessorUnitTest {

    private fun create_upstream_element(id: Long): OsmUpstreamElement {
        val r = OsmUpstreamElement(id)
        r.way = PGgeometry()
        r.tags = HashMap()
        return r
    }

    private fun create_osm_way(id: Long, nodes: List<Long>): Way {
        return Way(id, TLongArrayList(nodes.toLongArray()))
    }

    private data class Mocks(
            val osmUpstreamLineMapper: OsmUpstreamLineMapper = mockk<OsmUpstreamLineMapper>(),
            val osmUpstreamPolygonMapper: OsmUpstreamPolygonMapper = mockk<OsmUpstreamPolygonMapper>(),
            val osmWayMapper: OsmWayMapper = mockk<OsmWayMapper>(),
            val wayNodeMapper: WayNodeMapper = mockk<WayNodeMapper>(relaxed = true),
            val tagParser: TagParser = mockk<TagParser>(),
            val determineLayerWay: DetermineLayerWay = mockk<DetermineLayerWay>(),
            val proc: OsmWayProcessor = OsmWayProcessor(osmUpstreamLineMapper, osmUpstreamPolygonMapper, osmWayMapper, wayNodeMapper, tagParser, determineLayerWay)
    ) {
        init {
            every { osmWayMapper.bulkInsert(any()) } returns Unit
            every { tagParser.parse(hashMapOf()) } returns Tags()
        }
    }

    @Test
    fun basic_no_waynodes() {
        val mocks = Mocks()
        every { mocks.osmUpstreamLineMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf(10L to create_upstream_element(10))
        every { mocks.osmUpstreamPolygonMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf()
        every { mocks.determineLayerWay.determine(any(), any()) } returns hashSetOf(Layer.Address)
        every { mocks.determineLayerWay.importNodesForLayer(any()) } returns false

        val ways = listOf(create_osm_way(10, listOf(1L, 2L, 3L)))

        runBlocking {
            mocks.proc.processEntities(ways)
        }

        verify(exactly = 1) { mocks.osmWayMapper.bulkInsert(match { it.size == 1 && it[0].id == 10L && !it[0].hasOneWayRestriction && !it[0].hasReversedOneWay }) }
        verify(exactly = 1) { mocks.wayNodeMapper.bulkInsert(arrayListOf()) }
    }

    @Test
    fun basic_polygon() {
        val mocks = Mocks()
        every { mocks.osmUpstreamLineMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf()
        every { mocks.osmUpstreamPolygonMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf(10L to create_upstream_element(10))
        every { mocks.determineLayerWay.determine(any(), any()) } returns hashSetOf(Layer.Address)
        every { mocks.determineLayerWay.importNodesForLayer(any()) } returns false

        val ways = listOf(create_osm_way(10, listOf(1L, 2L, 3L)))

        runBlocking {
            mocks.proc.processEntities(ways)
        }

        verify(exactly = 1) { mocks.osmWayMapper.bulkInsert(match { it.size == 1 && it[0].id == 10L && !it[0].hasOneWayRestriction && !it[0].hasReversedOneWay }) }
        verify(exactly = 1) { mocks.wayNodeMapper.bulkInsert(arrayListOf()) }
    }

    @Test
    fun basic_no_layers() {
        val mocks = Mocks()
        every { mocks.osmUpstreamLineMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf(10L to create_upstream_element(10))
        every { mocks.osmUpstreamPolygonMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf()
        every { mocks.determineLayerWay.determine(any(), any()) } returns hashSetOf()
        every { mocks.determineLayerWay.importNodesForLayer(any()) } returns false

        val ways = listOf(create_osm_way(10, listOf(1L, 2L, 3L)))

        runBlocking {
            mocks.proc.processEntities(ways)
        }

        verify(exactly = 1) { mocks.osmWayMapper.bulkInsert(arrayListOf()) }
        verify(exactly = 1) { mocks.wayNodeMapper.bulkInsert(arrayListOf()) }
    }

    @Test
    fun basic_superfluous_layers() {
        val mocks = Mocks()
        every { mocks.osmUpstreamLineMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf(10L to create_upstream_element(10))
        every { mocks.osmUpstreamPolygonMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf()
        every { mocks.determineLayerWay.determine(any(), any()) } returns hashSetOf(Layer.Superfluous)
        every { mocks.determineLayerWay.importNodesForLayer(any()) } returns false

        val ways = listOf(create_osm_way(10, listOf(1L, 2L, 3L)))

        runBlocking {
            mocks.proc.processEntities(ways)
        }

        verify(exactly = 1) { mocks.osmWayMapper.bulkInsert(arrayListOf()) }
        verify(exactly = 1) { mocks.wayNodeMapper.bulkInsert(arrayListOf()) }
    }

    @Test
    fun basic_superfluous_layers2() {
        val mocks = Mocks()
        every { mocks.osmUpstreamLineMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf(10L to create_upstream_element(10))
        every { mocks.osmUpstreamPolygonMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf()
        every { mocks.determineLayerWay.determine(any(), any()) } returns hashSetOf(Layer.Superfluous, Layer.Address)
        every { mocks.determineLayerWay.importNodesForLayer(any()) } returns false

        val ways = listOf(create_osm_way(10, listOf(1L, 2L, 3L)))

        runBlocking {
            mocks.proc.processEntities(ways)
        }

        verify(exactly = 1) { mocks.osmWayMapper.bulkInsert(match { it.size == 1 && it[0].id == 10L && !it[0].hasOneWayRestriction && !it[0].hasReversedOneWay }) }
        verify(exactly = 1) { mocks.wayNodeMapper.bulkInsert(arrayListOf()) }
    }

    @Test
    fun basic_no_upstream() {
        val mocks = Mocks()
        every { mocks.osmUpstreamLineMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf()
        every { mocks.osmUpstreamPolygonMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf()
        every { mocks.determineLayerWay.determine(any(), any()) } returns hashSetOf(Layer.Superfluous, Layer.Address)
        every { mocks.determineLayerWay.importNodesForLayer(any()) } returns false

        val ways = listOf(create_osm_way(10, listOf(1L, 2L, 3L)))

        runBlocking {
            mocks.proc.processEntities(ways)
        }

        verify(exactly = 1) { mocks.osmWayMapper.bulkInsert(arrayListOf()) }
        verify(exactly = 1) { mocks.wayNodeMapper.bulkInsert(arrayListOf()) }
    }

    @Test
    fun multiple_layers() {
        val mocks = Mocks()
        every { mocks.osmUpstreamLineMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf(10L to create_upstream_element(10))
        every { mocks.osmUpstreamPolygonMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf()
        every { mocks.determineLayerWay.determine(any(), any()) } returns hashSetOf(Layer.Venue, Layer.Address)
        every { mocks.determineLayerWay.importNodesForLayer(any()) } returns false

        val ways = listOf(create_osm_way(10, listOf(1L, 2L, 3L)))

        val exception = assertThrows(Exception::class.java) {
            runBlocking {
                mocks.proc.processEntities(ways)
            }
        }
        assertEquals("Multiple layer found for 10 this should NOT Happen. Way will not be imported. Layers=Venue, Address", exception.message)

    }

    @Test
    fun multiple_upstreams() {
        val mocks = Mocks()
        every { mocks.osmUpstreamLineMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf(10L to create_upstream_element(10))
        every { mocks.osmUpstreamPolygonMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf(10L to create_upstream_element(10))

        val ways = listOf(create_osm_way(10, listOf(1L, 2L, 3L)))

        val exception = assertThrows(Exception::class.java) {
            runBlocking {
                mocks.proc.processEntities(ways)
            }
        }
        assertEquals("Both line and polygon found 10", exception.message)
    }

    @Test
    fun import_nodes() {
        val mocks = Mocks()
        every { mocks.osmUpstreamLineMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf(10L to create_upstream_element(10))
        every { mocks.osmUpstreamPolygonMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf()
        every { mocks.determineLayerWay.determine(any(), any()) } returns hashSetOf(Layer.Address)
        every { mocks.determineLayerWay.importNodesForLayer(any()) } returns true

        val ways = listOf(create_osm_way(10, listOf(1L, 2L, 3L)))

        runBlocking {
            mocks.proc.processEntities(ways)
        }

        val osmWayEntities = slot<ArrayList<be.ledfan.geocoder.db.entity.OsmWay>>()

        verify(exactly = 1) { mocks.osmWayMapper.bulkInsert(capture(osmWayEntities)) }
        assertEquals(1, osmWayEntities.captured.size)
        val osmWayEntity = osmWayEntities.captured[0]
        assertEquals(10L, osmWayEntity.id)

        verify(exactly = 1) {
            mocks.wayNodeMapper.bulkInsert(arrayListOf(
                    WayNodeMapper.BulkInsertData(osmWayEntity, 1, 0),
                    WayNodeMapper.BulkInsertData(osmWayEntity, 2, 1),
                    WayNodeMapper.BulkInsertData(osmWayEntity, 3, 2))
            )
        }
    }

    @Test
    fun basic_reverse_parsing() {
        fun setup(tags: HashMap<String, String>): be.ledfan.geocoder.db.entity.OsmWay {
            val mocks = Mocks()
            val dbUpObject = create_upstream_element(10)
            dbUpObject.tags = tags
            every { mocks.osmUpstreamLineMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf(10L to dbUpObject)
            every { mocks.osmUpstreamPolygonMapper.getByPrimaryIds(listOf(10)) } returns hashMapOf()
            every { mocks.determineLayerWay.determine(any(), any()) } returns hashSetOf(Layer.Address)

            every { mocks.tagParser.parse(hashMapOf()) } returns TagParser().parse(tags)

            every { mocks.determineLayerWay.importNodesForLayer(any()) } returns true

            val ways = listOf(create_osm_way(10, listOf(1L, 2L, 3L)))

            runBlocking {
                mocks.proc.processEntities(ways)
            }

            val osmWayEntities = slot<ArrayList<be.ledfan.geocoder.db.entity.OsmWay>>()

            verify(exactly = 1) { mocks.osmWayMapper.bulkInsert(capture(osmWayEntities)) }
            assertEquals(1, osmWayEntities.captured.size)
            val osmWayEntity = osmWayEntities.captured[0]
            assertEquals(10L, osmWayEntity.id)
            return osmWayEntity
        }

        var r = setup(hashMapOf("highway" to "motorway"))
        assertTrue(r.hasOneWayRestriction)
        assertFalse(r.hasReversedOneWay)

        r = setup(hashMapOf("junction" to "roundabout"))
        assertTrue(r.hasOneWayRestriction)
        assertFalse(r.hasReversedOneWay)

        r = setup(hashMapOf("junction" to "circular"))
        assertTrue(r.hasOneWayRestriction)
        assertFalse(r.hasReversedOneWay)

        r = setup(hashMapOf("oneway" to "yes"))
        assertTrue(r.hasOneWayRestriction)
        assertFalse(r.hasReversedOneWay)

        r = setup(hashMapOf("oneway" to "true"))
        assertTrue(r.hasOneWayRestriction)
        assertFalse(r.hasReversedOneWay)

        r = setup(hashMapOf("oneway" to "1"))
        assertTrue(r.hasOneWayRestriction)
        assertFalse(r.hasReversedOneWay)

        r = setup(hashMapOf("oneway" to "reverse"))
        assertTrue(r.hasOneWayRestriction)
        assertTrue(r.hasReversedOneWay)

        r = setup(hashMapOf("oneway" to "-1"))
        assertTrue(r.hasOneWayRestriction)
        assertTrue(r.hasReversedOneWay)

        r = setup(hashMapOf("junction" to "yes"))
        assertFalse(r.hasOneWayRestriction)
        assertFalse(r.hasReversedOneWay)

        r = setup(hashMapOf("highway" to "residential"))
        assertFalse(r.hasOneWayRestriction)
        assertFalse(r.hasReversedOneWay)

    }
}