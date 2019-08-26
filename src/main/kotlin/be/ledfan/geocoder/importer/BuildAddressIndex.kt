package be.ledfan.geocoder.importer

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.mapper.AddressIndexMapper
import be.ledfan.geocoder.db.mapper.OsmNodeMapper
import be.ledfan.geocoder.db.mapper.OsmParentMapper
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import be.ledfan.geocoder.geocoding.SearchTable
import be.ledfan.geocoder.importer.core.BaseProcessor
import be.ledfan.geocoder.importer.core.Broker
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.kodein
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.kodein.di.direct
import org.kodein.di.generic.instance
import kotlin.math.min

class AddressNodeProcessor(private val addressIndexMapper: AddressIndexMapper,
                           private val osmWayMapper: OsmWayMapper,
                           private val parentMapper: OsmParentMapper,
                           private val con: ConnectionWrapper) : BaseProcessor<OsmEntity>() {

    override suspend fun processEntities(entities: List<OsmEntity>) {
        val addressIndexes = HashMap<Long, AddressIndex>()
        val entitiesMap = HashMap(entities.associateBy { it.id })

        logger.debug { "Looking up parents and addressindex for ${entities.size} entities, going to find streets" }
        // get parents
        val parents = parentMapper.getParents(entities)
        logger.debug { "Done" }

        // determine basic properties
        for (entity in entities) {
            val addressIndex = AddressIndex.create(entity.id, SearchTable.Node)
            val parentsOfEntity = parents[entity.id] ?: continue
            for (parent in parentsOfEntity) {
                when (parent.layer) {
                    Layer.Neighbourhood -> {
                        addressIndex.neighbourhood_id = parent.id
                    }
                    Layer.LocalAdmin -> {
                        addressIndex.localadmin_id = parent.id
                    }
                    Layer.County -> {
                        addressIndex.county_id = parent.id
                    }
                    Layer.MacroRegion -> {
                        addressIndex.macroregion_id = parent.id
                    }
                    Layer.Country -> {
                        addressIndex.country_id = parent.id
                    }
                    else -> {
                    }
                }
            }
            addressIndexes[entity.id] = addressIndex
        }

        logger.debug { "Found parents and addressindex for ${entities.size} entities, going to find streets" }

        val (streetIds, houseNumbers) = findRelatedStreet(con, osmWayMapper, entitiesMap, addressIndexes)

        for ((id, addressIndex) in addressIndexes) {
//            if (streetIds[id] == 0L) {
//                throw Exception("Street is is 0 for enttiy $id, addressIndex: $addressIndex")
//            }
            addressIndex.street_id = streetIds[id]
            addressIndex.housenumber = houseNumbers[id]
        }

        addressIndexMapper.bulkInsert(addressIndexes.values.toList())
    }

}

class BuildAddressIndex(private val osmNodeMapper: OsmNodeMapper, private val osmWayMapper: OsmWayMapper, private val config: Config) {

    private val logger = KotlinLogging.logger {}

    suspend fun build() {

        // step 1 get all nodes with layer Address or Venue
        val nodes = osmNodeMapper.getAddressesAndVenues()
        // note: for larger imports this ^ may not fit into memory

        val broker = Broker<OsmEntity>(
                config.importer.outputThreshold,
                config.importer.numProcessors,
                config.importer.maxQueueSze,
                32000,
                { kodein.direct.instance<AddressNodeProcessor>() },
                "Node",
                "Step 5 --> Index Address",
                kodein.direct.instance())

//        broker.enqueueAll(nodes.values.toList())
        broker.startProcessors()

        // queue is now filled with nodes, fillt it with ways
        // step 2 get all nodes with layer Address or Venue
        delay(3000L)
        val ways = ArrayList(osmWayMapper.getAddressesAndVenues().values)
        logger.debug { "Found ${ways.size} addresses in ways table" }
        var oldIndex = 0
        while (oldIndex != ways.size) {
            val end = min(oldIndex + broker.freeSpace(), ways.size)
            logger.debug { "Ways remaining: ${ways.size - oldIndex}, can queue: $end items" }
            val waysToQueue = ways.subList(oldIndex, end)
            oldIndex = end
            logger.debug { "Got Sublist" }
            broker.enqueueAll(waysToQueue)
            logger.debug { "Enqueued" }
//            ways.removeAll(waysToQueue)
//            logger.debug { "Removed way" }
            delay(3000L)
        }

        broker.finishedReading()
        broker.join()

    }

}