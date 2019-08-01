package importer.processors

import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.entity.OsmUpstreamElement
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.mapper.*
import be.ledfan.geocoder.importer.DetermineLayerRelation
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.importer.core.Tags
import be.ledfan.geocoder.importer.processors.OsmRelationProcessor
import com.slimjars.dist.gnu.trove.list.array.TLongArrayList
import de.topobyte.osm4j.core.model.impl.Node
import de.topobyte.osm4j.core.model.impl.Relation
import de.topobyte.osm4j.core.model.impl.Way
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.postgis.PGgeometry
import kotlin.test.assertEquals

class OsmRelationProcessorUnitTest {

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

    private fun create_osm_relation(id: Long): Relation {
        return Relation(id, listOf())
    }

    private data class Mocks(
            val osmRelationMapper: OsmRelationMapper = mockk(),
            val osmUpstreamRelationMapper: OsmUpstreamPolygonMapper = mockk(),
            val tagParser: TagParser = mockk(),
            val determineLayerRelation: DetermineLayerRelation = mockk(),
            val proc: OsmRelationProcessor = OsmRelationProcessor(determineLayerRelation, tagParser, osmRelationMapper, osmUpstreamRelationMapper)
    ) {
        init {
            every { osmRelationMapper.bulkInsert(any()) } returns Unit
            every { tagParser.parse(hashMapOf()) } returns Tags()
        }
    }

    @Test
    fun no_associated_ways_and_point() {
        val mocks = Mocks()

        every { mocks.osmUpstreamRelationMapper.getByPrimaryIds(listOf(-10L)) } returns hashMapOf(-10L to create_upstream_element(-10L))
        every { mocks.determineLayerRelation.determine(any(), any()) } returns hashSetOf(Layer.LocalAdmin)

        val relations = listOf(create_osm_relation(10))

        runBlocking {
            mocks.proc.processEntities(relations)
        }

        verify(exactly = 1) { mocks.osmRelationMapper.bulkInsert(match { it.size == 1 && it[0].name == null && it[0].layer == Layer.LocalAdmin && it[0].id == 10L }) }
    }

    @Test
    fun parse_name() {
        fun setup(tags: HashMap<String, String>) : OsmRelation {
            val mocks = Mocks()

            val dbUpObject = create_upstream_element(-10L)
            dbUpObject.tags = tags

            every { mocks.osmUpstreamRelationMapper.getByPrimaryIds(listOf(-10L)) } returns hashMapOf(-10L to dbUpObject)
            every { mocks.determineLayerRelation.determine(any(), any()) } returns hashSetOf(Layer.LocalAdmin)
            every { mocks.tagParser.parse(hashMapOf()) } returns TagParser().parse(tags)

            val relations = listOf(create_osm_relation(10))

            runBlocking {
                mocks.proc.processEntities(relations)
            }

            val osmRelationEntities = slot<ArrayList<OsmRelation>>()

            verify(exactly = 1) { mocks.osmRelationMapper.bulkInsert(capture(osmRelationEntities)) }

            assertEquals(1, osmRelationEntities.captured.size)
            val osmRelationEntity = osmRelationEntities.captured[0]
            assertEquals(10L, osmRelationEntity.id)
            return osmRelationEntity
        }

        assertEquals("test", setup(hashMapOf("name" to "test")).name)
        assertEquals(null, setup(hashMapOf("name:fr" to "test", "name:nl" to "abc")).name)
    }

    @Test
    fun multiple_layers() {
        val mocks = Mocks()

        every { mocks.osmUpstreamRelationMapper.getByPrimaryIds(listOf(-10L)) } returns hashMapOf(-10L to create_upstream_element(-10L))

        every { mocks.determineLayerRelation.determine(any(), any()) } returns hashSetOf(Layer.LocalAdmin, Layer.Country)

        val relations = listOf(create_osm_relation(10))

        val exception = Assertions.assertThrows(Exception::class.java) {
            runBlocking {
                mocks.proc.processEntities(relations)
            }
        }
        assertEquals("Multiple layer found for 10 this should NOT Happen. Relation will not be imported. Layers=LocalAdmin, Country", exception.message)

    }

    @Test
    fun no_upstream() {
        val mocks = Mocks()

        every { mocks.osmUpstreamRelationMapper.getByPrimaryIds(listOf(-10L)) } returns hashMapOf()

        val relations = listOf(create_osm_relation(10))

        runBlocking {
            mocks.proc.processEntities(relations)
        }

        verify(exactly = 1) { mocks.osmRelationMapper.bulkInsert(arrayListOf()) }
        verify(exactly = 0) { mocks.determineLayerRelation.determine(any(), any()) }
    }

    @Test
    fun no_layer() {
        val mocks = Mocks()

        every { mocks.osmUpstreamRelationMapper.getByPrimaryIds(listOf(-10L)) } returns hashMapOf(-10L to create_upstream_element(-10L))
        every { mocks.determineLayerRelation.determine(any(), any()) } returns hashSetOf()

        val relations = listOf(create_osm_relation(10))

        runBlocking {
            mocks.proc.processEntities(relations)
        }

        verify(exactly = 1) { mocks.osmRelationMapper.bulkInsert(arrayListOf()) }
        verify(exactly = 1) { mocks.determineLayerRelation.determine(any(), any()) }
    }


}

