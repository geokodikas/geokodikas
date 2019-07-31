package be.ledfan.geocoder.importer.processors

import be.ledfan.geocoder.importer.DetermineLayerNode
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.core.BaseProcessor
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.importer.core.hasAtLeast

import be.ledfan.geocoder.db.entity.OsmUpstreamElement
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.entity.WayNode
import be.ledfan.geocoder.db.mapper.OsmNodeMapper
import be.ledfan.geocoder.db.mapper.OsmUpstreamPointMapper
import be.ledfan.geocoder.db.mapper.WayNodeMapper
import be.ledfan.geocoder.importer.core.copyVersionAndTags
import de.topobyte.osm4j.core.model.iface.OsmNode
import mu.KotlinLogging
import org.postgis.PGgeometry
import org.postgis.Point
import java.lang.Exception

import be.ledfan.geocoder.db.entity.OsmNode as dbOsmNode


/**
 * Process OsmNode objects.
 */
class OsmNodeProcessor(private val osmNodeMapper: OsmNodeMapper,
                       private val osmUpstreamPointMapper: OsmUpstreamPointMapper,
                       private val wayNodeMapper: WayNodeMapper,
                       private val determineLayer: DetermineLayerNode,
                       private val tagParser: TagParser) : BaseProcessor<OsmNode>() {

    private var logger = KotlinLogging.logger {}

    override suspend fun processEntities(entities: List<OsmNode>) {

        val osmIds = entities.map { it.id }
        val dbObjects: ArrayList<dbOsmNode> = ArrayList()

        val upstreamObjects = osmUpstreamPointMapper.getByPrimaryIds(osmIds)
        val linkedWaysByNode = wayNodeMapper.getLinkedWaysByNode(osmIds)

        val updatedLinkedWays = ArrayList<WayNode>()

        for (entity in entities) {
            val linkedWays = linkedWaysByNode[entity.id]
            val point = upstreamObjects[entity.id]

            if (point == null && (linkedWays == null || linkedWays.size == 0)) {
                // skip nodes not related to a interesting way
                logger.trace { "Skipping ${entity.id} because no associated ways and no point" }
                continue
            }

            val dbObject = dbOsmNode(entity.id)
            entity.copyVersionAndTags(dbObject)

            val pointAvailable = when (point) {
                null -> {
                    dbObject.centroid = PGgeometry(Point(entity.longitude, entity.latitude))
                    false
                }
                else -> {
                    dbObject.centroid = point.way
                    dbObject.zOrder = point.zOrder
                    true
                }
            }


            try {
                val tags = tagParser.parse(dbObject.tags)

                var layers = HashSet<Layer>()
                // if at least two ways with layer Street or Link -> assume this is a Junction
                // TODO check if it isn't the same highway
                if (linkedWays != null && linkedWays.hasAtLeast(2) { way -> way.second == Layer.Street || way.second == Layer.Link || way.second == Layer.Junction }) {
                    // assume it's a junction
                    logger.trace { "Assuming ${entity.id} is a junction because it has at least two linked ways" }
                    layers.add(Layer.Junction)
                } else {
                    layers = determineLayer.determine(dbObject, tags, pointAvailable)
                }

                layers.remove(Layer.Superfluous) // Remove superfluous layers, if layers left -> not superfluous

                when (layers.size) {
                    0 -> logger.trace { "Skipping ${dbObject.id} because no suitable layer found" }
                    1 -> {
                        dbObject.assignLayer(layers.first())
                        dbObjects.add(dbObject)
                        // updateWayNodes

                        if (linkedWays != null) {
                            for (linkedWay in linkedWays) {
                                val wayNode = WayNode.create(linkedWay.first, dbObject.id)
                                wayNode.nodeLayer = dbObject.layer
                                updatedLinkedWays.add(wayNode)
                            }
                        }
                    }
                    else -> logger.warn { "Multiple layer found for ${dbObject.id} this should NOT Happen. Node will not be imported. Layers=${layers.joinToString()}" }
                }
            } catch (e: Exception) {
                logger.error(e) {}
            }
        }
        osmNodeMapper.bulkInsert(dbObjects)
        wayNodeMapper.bulkUpdateNodeLayer(updatedLinkedWays)
    }

}
