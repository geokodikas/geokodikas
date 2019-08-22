package be.ledfan.geocoder.importer

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.entity.AddressIndex
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
import mu.KotlinLogging
import org.kodein.di.direct
import org.kodein.di.generic.instance

class AddressNodeProcessor(private val addressIndexMapper: AddressIndexMapper,
                           private val osmWayMapper: OsmWayMapper,
                           private val parentMapper: OsmParentMapper) : BaseProcessor<OsmNode>() {

    override suspend fun processEntities(entities: List<OsmNode>) {
        val addressIndexes = HashMap<Long, AddressIndex>()

        // get parents
        val parents = parentMapper.getParents(entities)

        // try to determine street

//            val parsedTags = tagParser.parse(entity.tags)

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
//            addressIndex.street_id = streetIds[entity.id]

            addressIndexes[entity.id] = addressIndex
        }

        val streetIds = findRelatedStreet(osmWayMapper, entities, addressIndexes)

        for ((id, addressIndex) in addressIndexes) {
            addressIndex.street_id = streetIds[id]
        }

        addressIndexMapper.bulkInsert(addressIndexes.values.toList())
    }

}

class BuildAddressIndex(private val osmNodeMapper: OsmNodeMapper, private val config: Config) {

    private val logger = KotlinLogging.logger {}

    suspend fun build() {

        // step 1 get all nodes with layer Address or Venue
        val nodes = osmNodeMapper.getAddressesAndVenues()
        // note: for larger imports this ^ may not fit into memory

        val broker = Broker<OsmNode>(
                config.importer.outputThreshold,
                config.importer.numProcessors,
                config.importer.maxQueueSze,
                config.importer.processorBlockSize,
                { kodein.direct.instance<AddressNodeProcessor>() },
                "Node",
                "Step 5 --> Index Address",
                kodein.direct.instance())

        broker.enqueueAll(nodes.values.toList())
        broker.startProcessors()
        broker.finishedReading()
        broker.join()

    }

}