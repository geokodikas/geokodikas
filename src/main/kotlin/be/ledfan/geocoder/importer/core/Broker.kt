package be.ledfan.geocoder.importer.core

import be.ledfan.geocoder.kodein
import de.topobyte.osm4j.core.model.iface.OsmEntity
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.timer

/**
 * The Broker class maintains a Queue and a collection of processors.
 */
class Broker<OsmType>(
        private val outputThreshold: Int,
        private val numProcessors: Int,
        private val maxQueueSize: Int,
        private val processorBlocKSize: Int,
        private val factory: () -> BaseProcessor<OsmType>,
        private val osmTypeName: String,
        private val statsLineKey: String,
        private val statsCollector: StatsCollector
) {

    private val queue = LinkedBlockingQueue<OsmType>(maxQueueSize)

    private var numRead = 0

    private val jobs = HashMap<Int, Job>()
    private val processors = ConcurrentHashMap<Int, BaseProcessor<OsmType>>()

    private var startTime = System.currentTimeMillis()

    private lateinit var statsTimer: Timer

    private val logger = KotlinLogging.logger {}

    /**
     * Starts $processorCount processors which the elements for this broker are send to.
     */
    suspend fun startProcessors() {
        logger.info { "Starting $numProcessors processors for type: $osmTypeName ..." }
        for (i in 0 until numProcessors) {
            val job = GlobalScope.launch(Dispatchers.IO) {
                try {
                    val processor = factory() // create processor
                    processors[i] = processor

                    processor.queue = queue
                    processor.threadId = i
                    processor.outputThreshold = outputThreshold
                    processor.blockSize = processorBlocKSize
                    processor.run() // start processing
                } catch (e: Exception) {
                    val importer = kodein.direct.instance<Importer>()
                    importer.stopWithFatalError(e)
                }
            }
            jobs[i] = job
        }
        logger.info { "$statsLineKey --> Started all processors, ${jobs.size}, ${processors.size}" }
        statsTimer = timer("print_stats_timer", period = 250) {
            updateStats()
        }
        while (processors.size != numProcessors) {
            delay(1000)
        }
        logger.info { "All processors eventually registered"}
    }

    /**
     * Enqueues an element for processing.
     */
    fun enqueue(el: OsmType) {
        numRead++

        queue.put(el)
    }

    fun enqueueAll(elements: List<OsmType>) {
        queue.addAll(elements)
        numRead += elements.size
        logger.debug {"queued some items, queue size: ${queue.size}"}
    }

    fun freeSpace(): Int {
        return maxQueueSize - queue.size
    }

    private fun updateStats() {
        val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000.0).toFloat()

        var totalProcessed = 0
        for (processor in processors.values) {
            totalProcessed += processor.processedCount
        }

        statsCollector.updateStatistics(statsLineKey, "Read", numRead)
        statsCollector.updateStatistics(statsLineKey, "Read/s", (numRead / elapsedSeconds).toInt())
        statsCollector.updateStatistics(statsLineKey, "Processed", totalProcessed)
        statsCollector.updateStatistics(statsLineKey, "Processed/s", (totalProcessed / elapsedSeconds).toInt())
        statsCollector.updateStatistics(statsLineKey, "QueueSize", queue.size)
    }

    /**
     * Must be called when finished with reading. After that the remaining items in the queue are processed and this
     * broker will stop.
     */
    fun finishedReading() {
        // without this function it could be the case that the queue is empty, but there are still items being read
        // in that case the processors may not stop.
        logger.info { "$statsLineKey --> Finished reading, processing remaining items in queue" }
        for (processor in processors.values) {
            processor.terminate()
        }

    }

    /**
     * Waits for the processors to stop.
     */
    suspend fun join() {
        for ((idx, job) in jobs) {
            logger.trace { "$statsLineKey --> Join job $idx" }
            job.join()
        }
        for (idx in 0 until numProcessors) {
            logger.trace { "$statsLineKey --> Finishing job $idx" }
            processors[idx]!!.finish()
            jobs[idx]!!.cancel()
        }
        statsTimer.cancel()
        updateStats()
        logger.info { "$statsLineKey --> Finished processing" }
    }

}
