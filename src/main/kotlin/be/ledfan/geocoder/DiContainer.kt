package be.ledfan.geocoder

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.config.ConfigReader
import be.ledfan.geocoder.db.ConnectionFactory
import be.ledfan.geocoder.db.mapper.*
import be.ledfan.geocoder.importer.*
import be.ledfan.geocoder.importer.core.Importer
import be.ledfan.geocoder.importer.core.StatsCollector
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.importer.processors.OsmNodeProcessor
import be.ledfan.geocoder.importer.processors.OsmRelationProcessor
import be.ledfan.geocoder.importer.processors.OsmWayProcessor
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import org.kodein.di.threadLocal
import java.sql.Connection

val kodein = Kodein {

    bind<Config>() with singleton { ConfigReader.getConfig() }

    bind<Connection>() with singleton(ref = threadLocal) { ConnectionFactory.createConnection(instance()) }

    bind<Importer>() with singleton { Importer() }

    bind<OsmNodeMapper>() with provider { OsmNodeMapper(instance()) }

    bind<OsmRelationMapper>() with provider { OsmRelationMapper(instance()) }

    bind<OsmUpstreamLineMapper>() with provider { OsmUpstreamLineMapper(instance()) }

    bind<OsmUpstreamPointMapper>() with provider { OsmUpstreamPointMapper(instance()) }

    bind<OsmUpstreamPolygonMapper>() with provider { OsmUpstreamPolygonMapper(instance()) }

    bind<OsmWayMapper>() with provider { OsmWayMapper(instance()) }

    bind<WayNodeMapper>() with provider { WayNodeMapper(instance()) }

    bind<OsmNodeProcessor>() with provider { OsmNodeProcessor(instance(), instance(), instance(), instance(), instance()) }

    bind<OsmRelationProcessor>() with provider { OsmRelationProcessor(instance(), instance(), instance(), instance()) }

    bind<OsmWayProcessor>() with provider { OsmWayProcessor(instance(), instance(), instance(), instance(), instance(), instance()) }

    bind<DetermineLayerNode>() with singleton { DetermineLayerNode() }

    bind<DetermineLayerRelation>() with singleton { DetermineLayerRelation() }

    bind<DetermineLayerWay>() with singleton { DetermineLayerWay() }

    bind<RelationHierarchyResolver>() with singleton { RelationHierarchyResolver(instance()) }

    bind<RelationPostProcessor>() with singleton { RelationPostProcessor(instance(), instance(), instance()) }

    bind<TagParser>() with singleton { TagParser() }

    bind<StatsCollector>() with singleton { StatsCollector() }

}