package be.ledfan.geocoder.importer.steps

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.importer.RegionPruner
import be.ledfan.geocoder.importer.RelationHierarchyResolver
import be.ledfan.geocoder.importer.RelationPostProcessor
import be.ledfan.geocoder.importer.core.Broker
import be.ledfan.geocoder.importer.core.StatsCollector
import be.ledfan.geocoder.importer.processors.OsmRelationProcessor
import be.ledfan.geocoder.kodein
import de.topobyte.osm4j.core.model.iface.OsmNode
import de.topobyte.osm4j.core.model.iface.OsmRelation
import de.topobyte.osm4j.core.model.iface.OsmWay
import de.topobyte.osm4j.pbf.seq.PbfIterator
import org.intellij.lang.annotations.Language
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.io.FileInputStream


suspend fun step3_checks(): Boolean {
    return ensureNotEmpty("osm_way") && ensureNotEmpty("osm_node") && ensureEmpty("osm_relation")
}


suspend fun step3_import_relations(): Boolean {
    val config: Config = kodein.direct.instance()
    val inputFileName = config.runtime.inputFileName
    val statsCollector: StatsCollector = kodein.direct.instance()

    val input = FileInputStream(inputFileName)

    val iterator = PbfIterator(input, true)

    val relationBroker: Broker<OsmRelation> = Broker(
            config.importer.outputThreshold,
            config.importer.numProcessors,
            config.importer.maxQueueSze,
            config.importer.processorBlockSize,
            { kodein.direct.instance<OsmRelationProcessor>() },
            "Relation",
            "Step 3 --> Relation",
            kodein.direct.instance())

    var nodeProcessed = 0
    var wayProcessed = 0
    statsCollector.updateStatistics("Step 3 --> Skip Node", "Skipping", nodeProcessed)
    statsCollector.updateStatistics("Step 3 --> Skip Way", "Skipping", wayProcessed)
    statsCollector.stopShowingSpinner()

    relationBroker.startProcessors()
    loop@ while (iterator.hasNext()) {
        val entity = iterator.next().entity
        when (entity) {
            is OsmNode -> {
                // ignore Node for now
                nodeProcessed++
                statsCollector.updateStatistics("Step 3 --> Skip Node", "Skipping", nodeProcessed)
            }
            is OsmWay -> {
                // ignore Node for now
                wayProcessed++
                statsCollector.updateStatistics("Step 3 --> Skip Way", "Skipping", wayProcessed)
            }
            is OsmRelation -> {
                relationBroker.enqueue(entity)
            }
        }
    }
    relationBroker.finishedReading()
    relationBroker.join()

    statsCollector.startShowingSpinner()
    return true

}

suspend fun step3_set_centroid(): Boolean {
    kodein.direct.instance<RelationPostProcessor>().setCentroid()
    return true
}


suspend fun step3_create_indexes(): Boolean {

    @Language("SQL")
    val sqlQuerries = listOf(
            "CREATE INDEX IF NOT EXISTS osm_relation_geometry_index ON osm_relation USING GIST(geometry)",
            "CREATE INDEX IF NOT EXISTS osm_relation_centroid_index ON osm_relation USING GIST(centroid)",
            "CREATE INDEX IF NOT EXISTS osm_relation_osm_id_index ON osm_relation(osm_id)",
            "CREATE INDEX IF NOT EXISTS osm_relation_geometry_layer_index ON osm_relation USING GIST(layer, geometry)",
            "CREATE INDEX IF NOT EXISTS parent_child_id_index ON parent (child_id)",
            "CREATE INDEX IF NOT EXISTS parent_parent_id_index ON parent (parent_id)"
    )


    return executeBatchQueries(sqlQuerries)

}

suspend fun step3_prune_regions(): Boolean {
    kodein.direct.instance<RegionPruner>().prune()
    return true
}

suspend fun step3_resolve_hierarchies(): Boolean {
    kodein.direct.instance<RelationHierarchyResolver>().run()
    return true
}
