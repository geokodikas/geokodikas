package be.ledfan.geocoder.importer

import be.ledfan.geocoder.db.entity.*
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.measureTimeMillisAndReturn
import mu.KotlinLogging
import kotlin.system.measureTimeMillis

val logger = KotlinLogging.logger {}

fun findRelatedStreet(belgium: Country, osmWayMapper: OsmWayMapper, entities: HashMap<Long, OsmEntity>, addressIndexes: HashMap<Long, AddressIndex>): Pair<java.util.HashMap<Long, Long?>, HashMap<Long, String?>> {

    // this function will first try to determine the method required to determine the street
    val nodesWithStreetNameAndLocalAdmin = ArrayList<Long>()
    val nodesWithoutStreetName = ArrayList<Long>()
    val houseNumbers = HashMap<Long, String?>()
    entities.values.forEach { houseNumbers[it.id] = null }

    for (entity in entities.values) {
        val parsedTags = TagParser().parse(entity.tags)

        if (parsedTags.hasChild("addr")) {
            val addrTag = parsedTags.child("addr")

            if (addrTag.hasChild("housenumber")) {
                houseNumbers[entity.id] = addrTag.child("housenumber").singleValueOrNull()
            }

            if (!addrTag.hasChild("street")) {
                nodesWithoutStreetName.add(entity.id)
                continue
            }

            val streetName = addrTag.child("street").singleValueOrNull()
            if (streetName == null) {
                nodesWithoutStreetName.add(entity.id)
                continue
            }

            nodesWithStreetNameAndLocalAdmin.add(entity.id)
        }
    }

    when (entities.values.first()) {
        is OsmNode -> {
            val result1 = measureTimeMillisAndReturn {
                osmWayMapper.getStreetsForNodes_FilterByWayNameAndLocalAdmin(nodesWithStreetNameAndLocalAdmin)
            }.let { (time, r) ->
                logger.debug{"Found streets for nodes in ${time}ms"}
                r
            }
            var closestStreetFallback = result1.filterValues { it == null || it == 0L }.keys

            // filter nodes not in belgium
            measureTimeMillis {
                closestStreetFallback = closestStreetFallback.filter { belgium.containsNode(entities[it] as OsmNode) }.toSet()
            }.let { time ->
                logger.debug{"Filtered nodes not in Belgium in ${time}ms"}
            }
            nodesWithoutStreetName.addAll(closestStreetFallback)

            logger.warn { "Found nodes with a street name without corresponding street way: $closestStreetFallback" }

            val result2 = osmWayMapper.getStreetsForNodes_FilterByClosestAndLocalAdmin(nodesWithoutStreetName)
            result1.putAll(result2)

            return Pair(result1, houseNumbers)
        }
        is OsmWay -> {
            val result1 = measureTimeMillisAndReturn {
                osmWayMapper.getStreetsForWays_FilterByWayNameAndLocalAdmin(nodesWithStreetNameAndLocalAdmin)
            }.let { (time, r) ->
                logger.debug{"Found streets for ways in ${time}ms"}
                r
            }

            var closestStreetFallback = result1.filterValues { it == null || it == 0L }.keys

            measureTimeMillis {
                closestStreetFallback = closestStreetFallback.filter { belgium.containsWay(entities[it] as OsmWay) }.toSet()
            }.let { time ->
                logger.debug{"Filtered ways not in Belgium in ${time}ms"}
            }
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