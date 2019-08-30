package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.*
import be.ledfan.geocoder.geo.Coordinate
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.kodein
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import org.kodein.di.direct
import org.kodein.di.generic.instance
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Coordinate as JtsCoordinate
import org.locationtech.jts.operation.distance.DistanceOp
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

private val reverseGeocoderContext = newFixedThreadPoolContext(16, "reverseGeocoderContext") // TODO make parameter configurable

class ReverseGeocoderService(private val reverseQueryBuilderFactory: ReverseQueryBuilderFactory) {


    suspend fun reverseGeocode(lat: Double, lon: Double,
                               limitNumeric: Int?, limitRadius: Int?,
                               desiredLayers: List<String>?):
            Result {

        val layers = if (desiredLayers == null || desiredLayers.isEmpty()) {
            Layer.values().toList()
        } else {
            desiredLayers.map { Layer.valueOf(it) }
        }

        val requiredTables = getTablesForLayers(layers)
        var entities = Collections.synchronizedList(ArrayList<OsmEntity>())

        val actualLimitNumeric = if (limitNumeric != null && limitNumeric > 0) {
            limitNumeric
        } else {
            5 // amount
        }

        val actualLimitRadius = if (limitRadius != null && limitRadius > 0) {
            limitRadius
        } else {
            200 // meter
        }

        withContext(reverseGeocoderContext) {
            for (table in requiredTables) {
                this@withContext.launch {
                    // query and process each table in parallel
                    val privateCon = kodein.direct.instance<ConnectionWrapper>()
                    val (sqlQuery, parameters) = reverseQueryBuilderFactory.createBuilder(table, debug = true).run {
                        baseQuery(lat, lon, actualLimitRadius, requiredTables, desiredLayers != null)
                        if (desiredLayers != null) {
                            whereLayer(layers)
                        }
                        orderBy()
                        limit(actualLimitNumeric)
                        build()
                    }

                    val stmt = privateCon.prepareStatement(sqlQuery)

                    parameters.forEachIndexed { index, param ->
                        stmt.setObject(index + 1, param)
                    }

                    val result = stmt.executeQuery()
                    while (result.next()) {
                        reverseQueryBuilderFactory.processResult(table, result, entities)
                    }

                    stmt.close()
                    result.close()
                }
            }
        }

        entities.sortBy { it.dynamicProperties["distance"] as Int }
        entities = ArrayList(entities.subList(0, min(actualLimitNumeric, entities.size)))

        val nodes = entities.filter { it.Type == OsmType.Node } as List<OsmNode>
        val ways = entities.filter { it.Type == OsmType.Way } as List<OsmWay>
        val relations = entities.filter { it.Type == OsmType.Relation } as List<OsmRelation>
        val address = entities.filter { it.Type == OsmType.AddressIndex } as List<AddressIndex>
        val closestEntity = entities.first()
        val inputGeometry = GeometryFactory().createPoint(JtsCoordinate(lon, lat))
        closestEntity.mainGeometry().value

        val distanceOp = DistanceOp(closestEntity.geometryAsJts(), inputGeometry)
        val closestPoint = distanceOp.nearestPoints().first()
        val order = entities.map { it.id }

        return Result(closestPoint, order, nodes, ways, relations, address)
    }

    data class Result(
            val closestPoint: JtsCoordinate,
            val order: List<Long>,
            val nodes: List<OsmNode>,
            val ways: List<OsmWay>,
            val relations: List<OsmRelation>,
            val addresses: List<AddressIndex>
    )

}