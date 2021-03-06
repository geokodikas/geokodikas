package be.ledfan.geocoder.importer

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.mapper.OsmNodeMapper
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import be.ledfan.geocoder.importer.core.Broker
import be.ledfan.geocoder.importer.processors.AddressNodeProcessor
import be.ledfan.geocoder.kodein
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.kodein.di.direct
import org.kodein.di.generic.instance
import kotlin.math.min

class BuildAddressIndex(private val osmNodeMapper: OsmNodeMapper, private val osmWayMapper: OsmWayMapper, private val config: Config) {

    private val logger = KotlinLogging.logger {}

    suspend fun build() {

        // step 1 get all nodes with layer Address or Venue
        val nodes = osmNodeMapper.getAddressesAndVenues()
        // note: for larger imports this ^ may not fit into memory

        fun createBroker(): Broker<OsmEntity> {
            return Broker<OsmEntity>(
                    config.importer.outputThreshold,
                    min(24, config.importer.numProcessors),
                    config.importer.maxQueueSze,
                    32000,
                    { kodein.direct.instance<AddressNodeProcessor>() },
                    "Node",
                    "Step 5 --> Index Address",
                    kodein.direct.instance())
        }

        val nodeBroker = createBroker()
        nodeBroker.enqueueAll(nodes)
        nodeBroker.startProcessors()
        nodeBroker.finishedReading()
        nodeBroker.join()

        // step 2 get ways nodes with layer Address or Venue

        val wayBroker = createBroker()
        wayBroker.startProcessors()

        val addresses = osmWayMapper.getAddressesAndVenuesWithPagination(config.importer.processorBlockSize)

        while (true) {
            val block = addresses.nextBlock()
            if (block.isEmpty()) break
            if (wayBroker.freeSpace() < block.size) {
                logger.debug { "Not enough space in queue, to enqueue new block" }
                delay(3000L)
                continue
            }
            logger.debug { "Enqueuing new block" }
            wayBroker.enqueueAll(block)
        }


        wayBroker.finishedReading()
        wayBroker.join()
    }

}