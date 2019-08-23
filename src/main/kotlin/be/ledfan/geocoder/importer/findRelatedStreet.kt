package be.ledfan.geocoder.importer

import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import be.ledfan.geocoder.importer.core.TagParser
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

fun findRelatedStreet(osmWayMapper: OsmWayMapper, entities: List<OsmEntity>, addressIndexes: HashMap<Long, AddressIndex>): Pair<java.util.HashMap<Long, Long?>, HashMap<Long, String?>> {

    // this function will first try to determine the method required to determine the street
    val nodesWithStreetNameAndLocalAdmin = ArrayList<Long>()
    val nodesWithoutStreetName = ArrayList<Long>()
    val houseNumbers = HashMap<Long, String?>()
    entities.forEach { houseNumbers[it.id] = null }

    for (entity in entities) {
        val parsedTags = TagParser().parse(entity.tags)

        if (parsedTags.hasChild("addr")) {
            val addrTag = parsedTags.child("addr")

            if (addrTag.hasChild("housenumber")) {
                houseNumbers[entity.id] = addrTag.child("housenumber").singleValueOrNull()
            }

            if (!addrTag.hasChild("street")) {
//                logger.warn("Entity with addr tag but not a street, id: ${entity.id}, tags: ${addrTag.toString(0)} ")
                nodesWithoutStreetName.add(entity.id)
                continue
            }

            val streetName = addrTag.child("street").singleValueOrNull()
            if (streetName == null) {
                nodesWithoutStreetName.add(entity.id)
                continue
//                logger.warn("Entity with addr tag street is null, id: ${entity.id}, tags: ${addrTag.toString(0)} ")
            }

            nodesWithStreetNameAndLocalAdmin.add(entity.id)
        }
    }

    when (entities.first()) {
        is OsmNode -> {
            val result1 = osmWayMapper.getStreetsForNodes_FilterByWayNameAndLocalAdmin(nodesWithStreetNameAndLocalAdmin)

            val closestStreetFallback = result1.filterValues { it == null || it == 0L }.keys
            logger.warn { "Found nodes with a street name without corresponding street way: $closestStreetFallback" }
            nodesWithoutStreetName.addAll(closestStreetFallback)

            val result2 = osmWayMapper.getStreetsForNodes_FilterByClosestAndLocalAdmin(nodesWithoutStreetName)

            result1.putAll(result2)

            return Pair(result1, houseNumbers)
        }
        is OsmWay -> {
            val result1 = osmWayMapper.getStreetsForWays_FilterByWayNameAndLocalAdmin(nodesWithStreetNameAndLocalAdmin)

            val closestStreetFallback = result1.filterValues { it == null || it == 0L }.keys
            nodesWithoutStreetName.addAll(closestStreetFallback)
            logger.warn { "Found way with a street name without corresponding street way: $closestStreetFallback" }

            val result2 = osmWayMapper.getStreetsForWays_FilterByClosestAndLocalAdmin(nodesWithoutStreetName)

            result1.putAll(result2)

            return Pair(result1, houseNumbers)
        }
        else -> {
            TODO()
        }
    }
}