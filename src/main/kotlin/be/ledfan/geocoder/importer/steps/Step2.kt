package be.ledfan.geocoder.importer.steps

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.ConnectionFactory
import be.ledfan.geocoder.db.mapper.WayNodeMapper
import be.ledfan.geocoder.importer.core.Broker
import be.ledfan.geocoder.importer.core.StatsCollector
import be.ledfan.geocoder.importer.processors.OsmNodeProcessor
import be.ledfan.geocoder.kodein
import de.topobyte.osm4j.core.model.iface.OsmNode
import de.topobyte.osm4j.pbf.seq.PbfIterator
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.io.FileInputStream


suspend fun step2_checks(): Boolean {
    return ensureEmpty("osm_node") && ensureNotEmpty("osm_way") && ensureNotEmpty("way_node")
}

suspend fun step2_import_nodes(): Boolean {

    val config: Config = kodein.direct.instance()
    val inputFileName = config.runtime.inputFileName
    val statsCollector: StatsCollector = kodein.direct.instance()

    val input = FileInputStream(inputFileName)

    val iterator = PbfIterator(input, true)

    val nodeBroker: Broker<OsmNode> = Broker(
            config.importer.outputThreshold,
            config.importer.numProcessors,
            config.importer.maxQueueSze,
            config.importer.processorBlockSize,
            { kodein.direct.instance<OsmNodeProcessor>() },
            "Node",
            "Step 2 --> Node",
            kodein.direct.instance())

    statsCollector.stopShowingSpinner()
    nodeBroker.startProcessors()
    loop@ while (iterator.hasNext()) {
        val entity = iterator.next().entity
        when (entity) {
            is OsmNode -> {
                nodeBroker.enqueue(entity)
            }
        }
    }
    nodeBroker.finishedReading()
    nodeBroker.join()

    statsCollector.startShowingSpinner()
    return true
}

suspend fun step2_create_indexes(): Boolean {
    // first create indexes on tables by way nodes
    @Language("SQL")
    val sqlQueries = listOf(
            "CREATE UNIQUE INDEX IF NOT EXISTS osm_node_osm_id_uindex ON osm_node (osm_id)",
            "CREATE INDEX IF NOT EXISTS osm_node_osm_centroid_uindex ON osm_node USING GIST(centroid)")

    return executeBatchQueries(sqlQueries)
}

//suspend fun step2_resolve_distances_way_node(): Boolean {
//    kodein.direct.instance<DistanceResolver>().run()
//    return true
//}


suspend fun step2_prune_nodes_without_layer(): Boolean {
    // Prune way_node entities with no node_layer assigned
    // -> this are either nodes which are used only as coordinate or which are uninteresting
    val wayNodeMapper: WayNodeMapper = kodein.direct.instance()
    val statsCollector: StatsCollector = kodein.direct.instance()
    val pruneCount = wayNodeMapper.pruneWithoutNodeLayer()

    statsCollector.updateStatistics("Step 2 --> Prune way_node", "Pruned", pruneCount)

    val pruneRelationCount = wayNodeMapper.pruneWayNodesRelatedToArealStreets()

    statsCollector.updateStatistics("Step 2 --> Prune way_node", "Pruned relation", pruneRelationCount)
    return true
}

//suspend fun step2_resolve_one_ways(): Boolean {
//
//    kodein.direct.instance<OneWayResolver>().run()
//
//    return true
//}

