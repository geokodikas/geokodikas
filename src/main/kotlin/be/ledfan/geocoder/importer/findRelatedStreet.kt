package be.ledfan.geocoder.importer

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.mapper.OsmWayMapper
import be.ledfan.geocoder.geo.toGeoJson
import be.ledfan.geocoder.importer.core.TagParser
import org.locationtech.jts.io.WKBReader
import mu.KotlinLogging
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

val logger = KotlinLogging.logger {}

fun findRelatedStreet(con: ConnectionWrapper, osmWayMapper: OsmWayMapper, entities: HashMap<Long, OsmEntity>, addressIndexes: HashMap<Long, AddressIndex>): Pair<java.util.HashMap<Long, Long?>, HashMap<Long, String?>> {

    val sql = "SELECT st_asbinary(geometry) as geometry from osm_relation where layer='Country'::Layer LIMIT 1"
    val stmt = con.prepareStatement(sql)
    val result = stmt.executeQuery()
    result.next()
    val belgium = WKBReader().read(result.getBinaryStream("geometry").readBytes())
    if (belgium == null || belgium.geometryType != "MultiPolygon") {
        throw Exception("Belgium is not a MultiPolygon")
    }
    val factory = GeometryFactory()

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

    when (entities.values.first()) {
        is OsmNode -> {
            val result1 = osmWayMapper.getStreetsForNodes_FilterByWayNameAndLocalAdmin(nodesWithStreetNameAndLocalAdmin)

            var closestStreetFallback = result1.filterValues { it == null || it == 0L }.keys
            logger.warn { "Found nodes with a street name without corresponding street way: $closestStreetFallback" }

            // filter nodes not in belgium

            closestStreetFallback = closestStreetFallback.filter {
                val centroid = (entities[it] as OsmNode).centroid.geometry as org.postgis.Point
                val point = factory.createPoint(Coordinate(centroid.x, centroid.y))
                point.within(belgium)
            }.toSet()
            nodesWithoutStreetName.addAll(closestStreetFallback)


//            val result2 = osmWayMapper.getStreetsForNodes_FilterByClosestAndLocalAdmin(nodesWithoutStreetName)

//            result1.putAll(result2)

            return Pair(result1, houseNumbers)
        }
        is OsmWay -> {
            val result1 = osmWayMapper.getStreetsForWays_FilterByWayNameAndLocalAdmin(nodesWithStreetNameAndLocalAdmin)

            var closestStreetFallback = result1.filterValues { it == null || it == 0L }.keys
            closestStreetFallback = closestStreetFallback.filter {
                val centroid = (entities[it] as OsmWay).centroid.geometry  as org.postgis.Point
                val point = factory.createPoint(Coordinate(centroid.x, centroid.y))
                point.within(belgium)
            }.toSet()
            nodesWithoutStreetName.addAll(closestStreetFallback)
            logger.warn { "Found way with a street name without corresponding street way: $closestStreetFallback" }

//            val result2 = osmWayMapper.getStreetsForWays_FilterByClosestAndLocalAdmin(nodesWithoutStreetName)

//            result1.putAll(result2)

            return Pair(result1, houseNumbers)
        }
        else -> {
            TODO()
        }
    }
}