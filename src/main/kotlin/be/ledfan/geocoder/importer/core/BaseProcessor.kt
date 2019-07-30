package be.ledfan.geocoder.importer.core

import de.topobyte.osm4j.core.model.iface.OsmEntity
import mu.KotlinLogging
import java.sql.Connection
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * For each OsmType a Processor is needed which for each element of that type will process it.
 * There will be multiple parallel processors for each type.
 */
abstract class BaseProcessor<OsmType>(private val connection: Connection) {

    @Volatile
    private var running: Boolean = true

    internal lateinit var queue: LinkedBlockingQueue<OsmType>
    internal var threadId: Int = 0
    internal var outputThreshold: Int = 0
    internal var blockSize: Int = 0

    private var logger = KotlinLogging.logger {}
    var processedCount: Int = 0

    /**
     * Starts reading the queue and processing each item in it.
     */
    suspend fun run() {
        logger.debug { "Processor $threadId started"}

        while (running || !queue.isEmpty()) {
            // while running (i.e. the main process is still reading items)
            // or when the queue is not empty (i.e. to finish the last items)
            // take an item of the queue and pass it to the specific process function

            val items: ArrayList<OsmType> = ArrayList()
            while (running && items.size < blockSize) {
                val el = queue.poll(1, TimeUnit.SECONDS) ?: continue
                items.add(el)
            }
            if (!running) {
                var el = queue.poll()
                while (el != null && items.size < blockSize) {
                    items.add(el)
                    el = queue.poll()
                }
            }

            if (items.size > 0) {
                processEntities(items)
            }
            processedCount += blockSize
        }
    }

    /**
     * Must be called when finished with reading. After that the remaining items in the queue are processed and this
     * processor will stop.
     */
    fun terminate() {
        running = false
    }

    fun finish() {
        logger.debug { "Processor $threadId stopped"}
    }

    abstract suspend fun processEntities(entities: List<OsmType>)


}
