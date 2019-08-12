package be.ledfan.geocoder.importer.processors

import be.ledfan.geocoder.db.entity.OsmUpstreamElement
import be.ledfan.geocoder.db.mapper.*
import be.ledfan.geocoder.importer.DetermineLayerNode
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.importer.core.Tags
import com.slimjars.dist.gnu.trove.list.array.TLongArrayList
import de.topobyte.osm4j.core.model.impl.Node
import de.topobyte.osm4j.core.model.impl.Way
import org.junit.jupiter.api.Assertions.assertThrows
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.postgis.PGgeometry
import org.postgis.Point

class OsmNodeProcessorUnitTest {

    private fun create_upstream_element(id: Long): OsmUpstreamElement {
        val r = OsmUpstreamElement(id)
        r.way = PGgeometry()
        r.tags = HashMap()
        return r
    }

    private fun create_osm_way(id: Long, nodes: List<Long>): Way {
        return Way(id, TLongArrayList(nodes.toLongArray()))
    }

    private fun create_osm_node(id: Long, lon: Double, lat: Double): Node {
        return Node(id, lon, lat)
    }

    private data class Mocks(
            val osmNodeMapper: OsmNodeMapper = mockk(),
            val osmUpstreamPointMapper: OsmUpstreamPointMapper = mockk(),
            val wayNodeMapper: WayNodeMapper = mockk(relaxed = true),
            val tagParser: TagParser = mockk(),
            val determineLayerNode: DetermineLayerNode = mockk(),
            val proc: OsmNodeProcessor = OsmNodeProcessor(osmNodeMapper, osmUpstreamPointMapper, wayNodeMapper, determineLayerNode, tagParser)
    ) {
        init {
            every { osmNodeMapper.bulkInsert(any()) } returns Unit
            every { wayNodeMapper.bulkUpdateNodeLayer(any()) } returns Unit
            every { tagParser.parse(hashMapOf()) } returns Tags()
        }
    }

    @Test
    fun no_associated_ways_and_point() {
        val mocks = Mocks()

        every { mocks.osmUpstreamPointMapper.getByPrimaryIds(listOf(10L)) } returns hashMapOf()
        every { mocks.wayNodeMapper.getLinkedWaysByNode(listOf(10L)) } returns hashMapOf()


        val nodes = listOf(create_osm_node(10, 1.0, 2.0))

        runBlocking {
            mocks.proc.processEntities(nodes)
        }


        verify(exactly = 1) { mocks.osmNodeMapper.bulkInsert(arrayListOf()) }
        verify(exactly = 1) { mocks.wayNodeMapper.bulkUpdateNodeLayer(arrayListOf()) }
    }

    @Test
    fun has_point() {
        val mocks = Mocks()

        val point = create_upstream_element(10)
        point.way = PGgeometry(Point(10.0, 12.0))
        point.zOrder = 42

        every { mocks.osmUpstreamPointMapper.getByPrimaryIds(listOf(10L)) } returns hashMapOf(10L to point)
        every { mocks.wayNodeMapper.getLinkedWaysByNode(listOf(10L)) } returns hashMapOf()
        every { mocks.determineLayerNode.determine(any(), any()) } returns hashSetOf(Layer.Street)

        val nodes = listOf(create_osm_node(10, 1.0, 2.0))

        runBlocking {
            mocks.proc.processEntities(nodes)
        }

        verify(exactly = 1) { mocks.osmNodeMapper.bulkInsert(match { it.size == 1 && it[0].zOrder == 42 && it[0].centroid == point.way }) }
        verify(exactly = 1) { mocks.wayNodeMapper.bulkUpdateNodeLayer(arrayListOf()) }
    }

    @Test
    fun no_point_but_has_ways() {
        val mocks = Mocks()

        val point = create_upstream_element(10)
        point.way = PGgeometry(Point(10.0, 12.0))
        point.zOrder = 42

        every { mocks.osmUpstreamPointMapper.getByPrimaryIds(listOf(10L)) } returns hashMapOf()
        every { mocks.wayNodeMapper.getLinkedWaysByNode(listOf(10L)) } returns hashMapOf(10L to arrayListOf(Pair(13L, Layer.Street)))
        every { mocks.determineLayerNode.determine(any(), any()) } returns hashSetOf(Layer.Junction)

        val node = create_osm_node(10, 1.0, 2.0)
        val nodes = listOf(node)

        runBlocking {
            mocks.proc.processEntities(nodes)
        }

        verify(exactly = 1) { mocks.osmNodeMapper.bulkInsert(match { it.size == 1 && it[0].zOrder == 0 && it[0].centroid == PGgeometry(Point(1.0, 2.0)) }) }
        verify(exactly = 1) { mocks.wayNodeMapper.bulkUpdateNodeLayer(match { it.size == 1 && it[0].nodeId == 10L && it[0].wayId == 13L }) }
    }

    @Test
    fun assume_junction() {
        val mocks = Mocks()

        val point = create_upstream_element(10)
        point.way = PGgeometry(Point(10.0, 12.0))
        point.zOrder = 42

        every { mocks.osmUpstreamPointMapper.getByPrimaryIds(listOf(10L)) } returns hashMapOf()
        every { mocks.wayNodeMapper.getLinkedWaysByNode(listOf(10L)) } returns hashMapOf(10L to arrayListOf(
                Pair(13L, Layer.Street),
                Pair(15L, Layer.Link)
        ))
        every { mocks.determineLayerNode.determine(any(), any()) } returns hashSetOf(Layer.Junction)

        val node = create_osm_node(10, 1.0, 2.0)
        val nodes = listOf(node)

        runBlocking {
            mocks.proc.processEntities(nodes)
        }

        verify(exactly = 1) {
            mocks.osmNodeMapper.bulkInsert(match {
                it.size == 1 && it[0].zOrder == 0 && it[0].centroid == PGgeometry(Point(1.0, 2.0))
                        && it[0].layer == Layer.Junction
            })
        }
        verify(exactly = 1) {
            mocks.wayNodeMapper.bulkUpdateNodeLayer(match {
                it.size == 2 && it[0].nodeId == 10L && it[0].wayId == 13L
                        && it[1].nodeId == 10L && it[1].wayId == 15L
            })
        }
        verify(exactly = 0) { mocks.determineLayerNode.determine(any(), any()) } // should assume it's a junction and don't call this function
    }

    @Test
    fun assume_junction2() {
        val mocks = Mocks()

        val point = create_upstream_element(10)
        point.way = PGgeometry(Point(10.0, 12.0))
        point.zOrder = 42

        every { mocks.osmUpstreamPointMapper.getByPrimaryIds(listOf(10L)) } returns hashMapOf()
        every { mocks.wayNodeMapper.getLinkedWaysByNode(listOf(10L)) } returns hashMapOf(10L to arrayListOf(
                Pair(13L, Layer.Link),
                Pair(15L, Layer.Junction),
                Pair(18L, Layer.Venue)
        ))
        every { mocks.determineLayerNode.determine(any(), any()) } returns hashSetOf(Layer.Junction)

        val node = create_osm_node(10, 1.0, 2.0)
        val nodes = listOf(node)

        runBlocking {
            mocks.proc.processEntities(nodes)
        }

        verify(exactly = 1) {
            mocks.osmNodeMapper.bulkInsert(match {
                it.size == 1 && it[0].zOrder == 0 && it[0].centroid == PGgeometry(Point(1.0, 2.0))
                        && it[0].layer == Layer.Junction
            })
        }
        verify(exactly = 1) {
            mocks.wayNodeMapper.bulkUpdateNodeLayer(match {
                it.size == 3 && it[0].nodeId == 10L && it[0].wayId == 13L
                        && it[1].nodeId == 10L && it[1].wayId == 15L
                        && it[2].nodeId == 10L && it[2].wayId == 18L
            })
        }
        verify(exactly = 0) { mocks.determineLayerNode.determine(any(), any()) } // should assume it's a junction and don't call this function
    }

    @Test
    fun basic_no_layers_and_no_junction() {
        val mocks = Mocks()

        val point = create_upstream_element(10)
        point.way = PGgeometry(Point(10.0, 12.0))
        point.zOrder = 42

        every { mocks.osmUpstreamPointMapper.getByPrimaryIds(listOf(10L)) } returns hashMapOf(10L to point)
        every { mocks.wayNodeMapper.getLinkedWaysByNode(listOf(10L)) } returns hashMapOf()
        every { mocks.determineLayerNode.determine(any(), any()) } returns hashSetOf()

        val nodes = listOf(create_osm_node(10, 1.0, 2.0))

        runBlocking {
            mocks.proc.processEntities(nodes)
        }

        verify(exactly = 1) { mocks.osmNodeMapper.bulkInsert(arrayListOf()) }
        verify(exactly = 1) { mocks.wayNodeMapper.bulkUpdateNodeLayer(arrayListOf()) }

    }

    @Test
    fun multiple_layers() {
        val mocks = Mocks()

        val point = create_upstream_element(10)

        every { mocks.osmUpstreamPointMapper.getByPrimaryIds(listOf(10L)) } returns hashMapOf(10L to point)
        every { mocks.wayNodeMapper.getLinkedWaysByNode(listOf(10L)) } returns hashMapOf()
        every { mocks.determineLayerNode.determine(any(), any()) } returns hashSetOf(Layer.Junction, Layer.Address, Layer.Superfluous)

        val nodes = listOf(create_osm_node(10, 1.0, 2.0))

        val exception = assertThrows(Exception::class.java) {
            runBlocking {
                mocks.proc.processEntities(nodes)
            }
        }
        assertEquals("Multiple layer found for 10 this should NOT Happen. Node will not be imported. Layers=Junction, Address", exception.message)
    }

}

