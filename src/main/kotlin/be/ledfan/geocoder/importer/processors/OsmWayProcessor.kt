package be.ledfan.geocoder.importer.processors

import be.ledfan.geocoder.db.mapper.*
import be.ledfan.geocoder.importer.DetermineLayerWay
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.core.BaseProcessor
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.importer.core.Tags
import be.ledfan.geocoder.importer.core.copyVersionAndTags
import de.topobyte.osm4j.core.model.iface.OsmWay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import be.ledfan.geocoder.db.entity.OsmWay as dbOsmWay

/**
 * Process OsmWay objects.
 */
class OsmWayProcessor(private val osmUpstreamLineMapper: OsmUpstreamLineMapper,
                      private val osmUpstreamPolygonMapper: OsmUpstreamPolygonMapper,
                      private val osmWayMapper: OsmWayMapper,
                      private val wayNodeMapper: WayNodeMapper,
                      private val tagParser: TagParser,
                      private val determineLayer: DetermineLayerWay) : BaseProcessor<OsmWay>() {

    private val logger = KotlinLogging.logger {}

    override suspend fun processEntities(entities: List<OsmWay>) {

        val osmIds = entities.map { it.id }
        val dbObjects: ArrayList<dbOsmWay> = ArrayList()
        val wayNodes: ArrayList<WayNodeMapper.BulkInsertData> = ArrayList()

        val upstreamLineObjects = osmUpstreamLineMapper.getByPrimaryIds(osmIds)
        val upstreamPolygonObjects = osmUpstreamPolygonMapper.getByPrimaryIds(osmIds)

        loop@ for (entity in entities) {
            val line = upstreamLineObjects[entity.id]
            val polygon = upstreamPolygonObjects[entity.id]

            val upObject = when {
                line != null && polygon != null -> throw Exception("Both line and polygon found ${entity.id}")
                line != null -> line
                polygon != null -> polygon
                else -> {
                    logger.trace { "Skipping way ${entity.id} because not found in upstream db" }
                    continue@loop
                }
            }

            val dbObject = dbOsmWay(entity.id)
            entity.copyVersionAndTags(dbObject)
            dbObject.geometry = upObject.way
            dbObject.zOrder = upObject.zOrder

            val nodeIds = ArrayList<Long>()
            for (i in 0 until entity.numberOfNodes) {
                val nodeId = entity.getNodeId(i)
                nodeIds.add(nodeId)
            }

            val tags = tagParser.parse(dbObject.tags)
            val layers = determineLayer.determine(dbObject, tags)

            layers.remove(Layer.Superfluous) // Remove superfluous layers, if layers left -> not superfluous

            when (layers.size) {
                0 -> logger.trace { "Skipping ${dbObject.id} because no suitable layer found" }
                1 -> {
                    dbObject.assignLayer(layers.first())
                    checkAndProcessOneWay(dbObject, tags)
                    dbObjects.add(dbObject)
                    if (determineLayer.importNodesForLayer(layers.first())) {
                        for (idx in 0 until nodeIds.size) {
                            wayNodes.add(WayNodeMapper.BulkInsertData(dbObject, nodeIds[idx], idx))
                        }
                    }
                }
                else -> throw Exception("Multiple layer found for ${dbObject.id} this should NOT Happen. Way will not be imported. Layers=${layers.sorted().joinToString()}")
            }
        }

        osmWayMapper.bulkInsert(dbObjects)
        wayNodeMapper.bulkInsert(wayNodes)
    }

    private fun checkAndProcessOneWay(dbObject: be.ledfan.geocoder.db.entity.OsmWay, tags: Tags) {
        dbObject.hasOneWayRestriction = tags.childOrNull("highway")?.singleValueOrNull() == "motorway" ||
                tags.childOrNull("junction")?.singleValueOrNull() == "roundabout" ||
                tags.childOrNull("junction")?.singleValueOrNull() == "circular" ||
                tags.childOrNull("oneway")?.singleValueOrNull() == "yes" ||
                tags.childOrNull("oneway")?.singleValueOrNull() == "true" ||
                tags.childOrNull("oneway")?.singleValueOrNull() == "reverse" ||
                tags.childOrNull("oneway")?.singleValueOrNull() == "1" ||
                tags.childOrNull("oneway")?.singleValueOrNull() == "-1"

        if (!dbObject.hasOneWayRestriction) {
            return
        }

        dbObject.hasReversedOneWay = tags.childOrNull("oneway")?.singleValueOrNull() == "reverse"
                || tags.childOrNull("oneway")?.singleValueOrNull() == "-1"

    }

}
