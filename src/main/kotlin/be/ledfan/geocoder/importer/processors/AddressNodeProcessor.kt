package be.ledfan.geocoder.importer.processors

import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.Country
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.mapper.AddressIndexMapper
import be.ledfan.geocoder.db.mapper.OsmParentMapper
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.core.BaseProcessor
import be.ledfan.geocoder.importer.findRelatedStreet
import be.ledfan.geocoder.measureTimeMillisAndReturn
import mu.KotlinLogging
import kotlin.system.measureTimeMillis

class AddressNodeProcessor(private val country: Country,
                           private val addressIndexMapper: AddressIndexMapper,
                           private val osmWayMapper: OsmWayMapper,
                           private val parentMapper: OsmParentMapper) : BaseProcessor<OsmEntity>() {

    private val logger = KotlinLogging.logger {}

    override suspend fun processEntities(entities: List<OsmEntity>) {
        val addressIndexes = HashMap<Long, AddressIndex>()
        val entitiesMap = HashMap(entities.associateBy { it.id })

        // get parents
        val (time, parents) = measureTimeMillisAndReturn {
            parentMapper.getParents(entities)
        }

        be.ledfan.geocoder.importer.logger.debug { "Found parents for ${entities.size} entities in ${time}ms" }

        // determine basic properties
        for (entity in entities) {
            val addressIndex = AddressIndex.create(entity.id, entity.Type)
            addressIndex.tags = entity.tags
            addressIndex.geometry = entity.mainGeometry()
            addressIndex.layer = entity.layer
            val parentsOfEntity = parents[entity.id] ?: continue
            for (parent in parentsOfEntity) {
                when (parent.layer) {
                    Layer.Neighbourhood -> {
                        addressIndex.neighbourhoodId = parent.id
                    }
                    Layer.LocalAdmin -> {
                        addressIndex.localadminId = parent.id
                    }
                    Layer.County -> {
                        addressIndex.countyId = parent.id
                    }
                    Layer.MacroRegion -> {
                        addressIndex.macroregionId = parent.id
                    }
                    Layer.Country -> {
                        addressIndex.countryId = parent.id
                    }
                    else -> {
                    }
                }
            }
            addressIndexes[entity.id] = addressIndex
        }

        measureTimeMillis {
            findRelatedStreet(country, osmWayMapper, entitiesMap, addressIndexes)
        }.let { time ->
            logger.debug { "Found related streets for ${entities.size} entities in ${time}ms" }
        }

        addressIndexMapper.bulkInsert(addressIndexes.values.toList())
    }

}