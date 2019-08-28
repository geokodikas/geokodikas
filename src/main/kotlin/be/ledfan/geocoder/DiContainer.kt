package be.ledfan.geocoder

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.config.ConfigReader
import be.ledfan.geocoder.db.ConnectionFactory
import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.Country
import be.ledfan.geocoder.db.mapper.*
import be.ledfan.geocoder.httpapi.AddressController
import be.ledfan.geocoder.httpapi.OsmEntityController
import be.ledfan.geocoder.importer.*
import be.ledfan.geocoder.importer.core.Importer
import be.ledfan.geocoder.importer.core.StatsCollector
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.importer.processors.OsmNodeProcessor
import be.ledfan.geocoder.importer.processors.OsmRelationProcessor
import be.ledfan.geocoder.importer.processors.OsmWayProcessor
import be.ledfan.geocoder.httpapi.OverviewController
import be.ledfan.geocoder.httpapi.ReverseController
import io.ktor.locations.KtorExperimentalLocationsAPI
import org.kodein.di.conf.ConfigurableKodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import org.kodein.di.threadLocal


@UseExperimental(KtorExperimentalLocationsAPI::class)
val kodein = ConfigurableKodein().also {

    it.addConfig {

        /**
         *  DB
         */

        bind<Config>() with singleton { ConfigReader.getConfig() }

        bind<ConnectionWrapper>() with singleton(ref = threadLocal) { ConnectionFactory.createWrappedConnection(instance()) }

        bind<OsmNodeMapper>() with provider { OsmNodeMapper(instance()) }

        bind<OsmRelationMapper>() with provider { OsmRelationMapper(instance()) }

        bind<OsmUpstreamLineMapper>() with provider { OsmUpstreamLineMapper(instance()) }

        bind<OsmUpstreamPointMapper>() with provider { OsmUpstreamPointMapper(instance()) }

        bind<OsmUpstreamPolygonMapper>() with provider { OsmUpstreamPolygonMapper(instance()) }

        bind<OsmWayMapper>() with provider { OsmWayMapper(instance()) }

        bind<WayNodeMapper>() with provider { WayNodeMapper(instance()) }

        bind<OsmParentMapper>() with provider { OsmParentMapper(instance()) }

        bind<AddressIndexMapper>() with provider { AddressIndexMapper(instance(), instance(), instance()) }

        /**
         * Importer
         */

        bind<Importer>() with singleton { Importer() }

        bind<OsmNodeProcessor>() with provider { OsmNodeProcessor(instance(), instance(), instance(), instance(), instance()) }

        bind<OsmRelationProcessor>() with provider { OsmRelationProcessor(instance(), instance(), instance(), instance()) }

        bind<OsmWayProcessor>() with provider { OsmWayProcessor(instance(), instance(), instance(), instance(), instance(), instance()) }

        bind<AddressNodeProcessor>() with provider { AddressNodeProcessor(instance(), instance(), instance(), instance(), instance()) }

        bind<DetermineLayerNode>() with singleton { DetermineLayerNode() }

        bind<DetermineLayerRelation>() with singleton { DetermineLayerRelation() }

        bind<DetermineLayerWay>() with singleton { DetermineLayerWay() }

        bind<RelationHierarchyResolver>() with singleton { RelationHierarchyResolver(instance()) }

        bind<RelationPostProcessor>() with singleton { RelationPostProcessor(instance(), instance(), instance()) }

        bind<RegionPruner>() with singleton { RegionPruner(instance(), instance(), instance(), instance(), instance()) }

        bind<StatsCollector>() with singleton { StatsCollector() }

        bind<BuildAddressIndex>() with singleton { BuildAddressIndex(instance(), instance(), instance()) }


        /**
         * Other
         */
        bind<TagParser>() with singleton { TagParser() }
        bind<Country>() with singleton { Country.getFromDb(instance(), 52411L) } // TODO magic number
        bind<HumanAddressBuilderService>() with provider { HumanAddressBuilderService(instance(), instance()) }

        /**
         * Controllers
         * TODO do we really want these to be singletons?
         */
        bind<OverviewController>() with singleton { OverviewController(this@singleton.kodein) }
        bind<ReverseController>() with singleton { ReverseController(this@singleton.kodein) }
        bind<OsmEntityController>() with singleton { OsmEntityController(this@singleton.kodein) }
        bind<AddressController>() with singleton { AddressController(this@singleton.kodein) }


    }
}