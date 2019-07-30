package be.ledfan.geocoder.importer.processors

import be.ledfan.geocoder.importer.core.BaseProcessor
//import be.ledfan.geocoder.db.entity.OneWayRestriction
//import be.ledfan.geocoder.db.mapper.*
import de.topobyte.osm4j.core.model.iface.OsmWay
import mu.KotlinLogging
import java.sql.Connection

/**
 * Process OsmWay objects.
 */
class OsmWayProcessor(
//        private val osmUpstreamLineMapper: OsmUpstreamLineMapper,
//                      private val osmUpstreamPolygonMapper: OsmUpstreamPolygonMapper,
//                      private val osmWayMapper: OsmWayMapper,
//                      private val wayNodeMapper: WayNodeMapper,
//                      private val oneWayRestrictionMapper: OneWayRestrictionMapper,
//                      private val tagParser: TagParser,
//                      private val determineLayer: DetermineLayerWay,
                      connection: Connection) : BaseProcessor<OsmWay>(connection) {


//    private val oneWayRestrictionObjects = ArrayList<OneWayRestriction>()

    private val logger = KotlinLogging.logger {}

    override suspend fun processOneEntity(node: List<OsmWay>) {

//        val osmIds = ArrayList<Long>()
//
//        val dbObjects: ArrayList<dbOsmWay> = ArrayList()
//
//        val wayNodes: ArrayList<WayNodeMapper.BulkInsertData> = ArrayList()
//
//        node.forEach {
//            osmIds.add(it.id)
//        }
//
//        val upstreamLineObjects = osmUpstreamLineMapper.getByPrimaryIds(osmIds)
//        val upstreamPolygonObjects = osmUpstreamPolygonMapper.getByPrimaryIds(osmIds)
//
//        loop@ for (it in node) {
//            val line = upstreamLineObjects[it.id] as OsmUpstreamElement?
//            val polygon = upstreamPolygonObjects[it.id] as OsmUpstreamElement?
//
//            val upObject = when {
//                line != null && polygon != null -> throw Exception("Both line and polygon found ${it.id}")
//                line != null -> line
//                polygon != null -> polygon
//                else -> {
//                    logger.trace { "Skipping way ${it.id} because not found in upstream db" }
//                    continue@loop
//                }
//            }
//
//            val dbObject = dbOsmWay(it.id)
//
//            it.metadata?.version?.let {
//                dbObject.version = it
//            }
//
//            if (it.numberOfTags > 0) {
//                for (i in 0 until it.numberOfTags) {
//                    dbObject.tags[it.getTag(i).key] = it.getTag(i).value
//                }
//            }
//
//            dbObject.geometry = upObject.way
//            dbObject.zOrder = upObject.zOrder
//
//            val nodeIds = ArrayList<Long>()
//            for (i in 0 until it.numberOfNodes) {
//                val nodeId = it.getNodeId(i)
//                nodeIds.add(nodeId)
//            }
//
//            try {
//                val tags = tagParser.parse(dbObject)
//                val layers = determineLayer.determine(dbObject, tags)
//
//                layers.remove(Layer.Superfluous) // Remove superfluous layers, if layers left -> not superfluous
//
//                when (layers.size) {
//                    0 -> logger.trace { "Skipping ${dbObject.id} because no suitable layer found" }
//                    1 -> {
//                        dbObject.assignLayer(layers.first())
//                        checkAndProcessOneWay(dbObject, tags)
//                        dbObjects.add(dbObject)
//                        if (determineLayer.importNodesForLayer(layers.first())) {
//                            for (idx in 0 until nodeIds.size) {
//                                wayNodes.add(WayNodeMapper.BulkInsertData(dbObject, nodeIds[idx], idx))
//                            }
//                        }
//                    }
//                    else -> logger.warn { "Multiple layer found for ${dbObject.id} this should NOT Happen. Way will not be imported. Layers=${layers.joinToString()}" }
//                }
//            } catch (e: java.lang.Exception) {
//                logger.error(e) {}
//            }
//        }

//        osmWayMapper.bulkInsert(dbObjects)
//        wayNodeMapper.bulkInsert(wayNodes)
//        oneWayRestrictionMapper.bulkInsert(oneWayRestrictionObjects)
//        oneWayRestrictionObjects.clear()
    }

//    private fun checkAndProcessOneWay(dbObject: be.ledfan.geocoder.db.entity.OsmWay, tags: Tags) {
//        dbObject.hasOneWayRestriction = when {
//            (tags.hasChild("highway") && tags.getSingleValueOfChild("highway") == "motorway") -> true
//            (tags.hasChild("junction") && tags.getSingleValueOfChild("junction") == "roundabout") -> true
//            (tags.hasChild("junction") && tags.getSingleValueOfChild("junction") == "circular") -> true
//            (tags.hasChild("oneway") && (tags.getSingleValueOfChild("oneway") == "yes" || tags.getSingleValueOfChild("oneway") == "1" || tags.getSingleValueOfChild("oneway") == "true")) -> true
//            else -> false;
//        }
//
//        if (!dbObject.hasOneWayRestriction) {
//            return
//        }
//
//        dbObject.hasReversedOneWay = tags.hasChild("oneway") && (tags.getSingleValueOfChild("oneway") == "-1" || tags.getSingleValueOfChild("oneway") == "reverse")
//    }

}
