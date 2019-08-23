@file:Suppress("FunctionName")

package be.ledfan.geocoder.importer.steps

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.ConnectionFactory
import be.ledfan.geocoder.importer.RegionPruner
import be.ledfan.geocoder.importer.RelationHierarchyResolver
import be.ledfan.geocoder.importer.core.Broker
import be.ledfan.geocoder.importer.processors.OsmWayProcessor
import be.ledfan.geocoder.importer.core.StatsCollector
import be.ledfan.geocoder.kodein
import de.topobyte.osm4j.core.model.iface.OsmNode
import de.topobyte.osm4j.core.model.iface.OsmRelation
import de.topobyte.osm4j.core.model.iface.OsmWay
import de.topobyte.osm4j.pbf.seq.PbfIterator
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.io.FileInputStream
import java.sql.Connection

private val logger = KotlinLogging.logger {}

suspend fun step1_checks(): Boolean {

    return ensureEmpty("osm_way") && ensureEmpty("way_node")
}


suspend fun step1_import_ways(): Boolean {

    val config: Config = kodein.direct.instance()
    val inputFileName = config.runtime.inputFileName
    val statsCollector: StatsCollector = kodein.direct.instance()

    val input = FileInputStream(inputFileName)

    val iterator = PbfIterator(input, true)

    val wayBroker: Broker<OsmWay> = Broker(
            config.importer.outputThreshold,
            config.importer.numProcessors,
            config.importer.maxQueueSze,
            config.importer.processorBlockSize,
            { kodein.direct.instance<OsmWayProcessor>() },
            "Way",
            "Step 1 --> Way",
            kodein.direct.instance())

    var nodeProcessed = 0
    statsCollector.stopShowingSpinner()
    statsCollector.updateStatistics("Step 1 --> Skip Node", "Skipping", nodeProcessed)
    wayBroker.startProcessors()

    loop@ while (iterator.hasNext()) {
        val entity = iterator.next().entity
        when (entity) {
            is OsmNode -> {
                // ignore Node for now
                nodeProcessed++
                statsCollector.updateStatistics("Step 1 --> Skip Node", "Skipping", nodeProcessed)
            }
            is OsmWay -> wayBroker.enqueue(entity)
            is OsmRelation -> {
                // stop processing if we encounter a relation
                break@loop
            }
        }
    }

    wayBroker.finishedReading()
    wayBroker.join()
    statsCollector.startShowingSpinner()

    return true
}

suspend fun step1_create_indexes(): Boolean {
    logger.info { "Updating centroids of osm_way and creating indexes" }

    @Language("SQL")
    val sqlQueries = listOf(
            "UPDATE osm_way SET centroid=st_centroid(geometry)",
            "CREATE UNIQUE INDEX IF NOT EXISTS osm_way_osm_id_index ON osm_way (osm_id)",
            "CREATE INDEX IF NOT EXISTS way_node_node_id_index ON way_node (node_id)",
            "CREATE INDEX IF NOT EXISTS way_node_way_id_index ON way_node (way_id)",
            "CREATE INDEX IF NOT EXISTS osm_way_centroid_index ON osm_way USING GIST(centroid)",
            "CREATE INDEX IF NOT EXISTS osm_way_geometry_index ON osm_way USING GIST(geometry)",
            "CREATE INDEX IF NOT EXISTS osm_way_geometry_layer_index ON osm_way USING GIST(layer, geometry)")
//        "CREATE INDEX IF NOT EXISTS one_way_restrictions_way_id_from_node_id_index ON one_way_restrictions (way_id, from_node_id)")

    return executeBatchQueriesParallel(sqlQueries)
}

