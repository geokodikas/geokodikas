package be.ledfan.geocoder.importer

import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import be.ledfan.geocoder.importer.core.TagParser
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

fun findRelatedStreet(osmWayMapper: OsmWayMapper, entities: List<OsmNode>, addressIndexes: HashMap<Long, AddressIndex>): HashMap<Long, Long> {


    val nodesWithStreetNameAndLocalAdmin = ArrayList<Long>()

    for (entity in entities) {

        val parsedTags = TagParser().parse(entity.tags)

        if (parsedTags.hasChild("addr")) {
            val addrTag = parsedTags.child("addr")
            if (!addrTag.hasChild("street")) {
                logger.warn("Entity with addr tag but not a street, id: ${entity.id}, tags: ${addrTag.toString(0)} ")
                continue
//                return null
            }

            val streetName = addrTag.child("street").singleValueOrNull()
            if (streetName == null) {
                logger.warn("Entity with addr tag street is null, id: ${entity.id}, tags: ${addrTag.toString(0)} ")
                continue
//                return null
            }
            nodesWithStreetNameAndLocalAdmin.add(entity.id)

            // find street
//        val streets = if (neighbourhoodId == null) {
//            val streets = osmWayMapper.getStreetForNodeByLocalAdmin(entity.id, localAdmin, streetName)
//        } else {
//            osmWayMapper.getStreetForNodeByNeighbourhood(entity.id, neighbourhoodId, streetName)
//        }
//            if (streets.isEmpty()) {
//                logger.warn("Found no street for entity ${entity.id}")
////                return null
//                continue
//            }
//            logger.info("Found street for entity ${entity.id} ${streets.first().wayId}")

//            return streets.first().wayId
        }
//        println("Entity ${entity.id} without addr tag")

    }

    return osmWayMapper.getStreetsForNodes_FilterByWayNameAndLocalAdmin(nodesWithStreetNameAndLocalAdmin)


}