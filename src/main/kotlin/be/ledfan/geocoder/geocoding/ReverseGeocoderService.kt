package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.kodein
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import org.kodein.di.direct
import org.kodein.di.generic.instance
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.operation.distance.DistanceOp
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min
import org.locationtech.jts.geom.Coordinate as JtsCoordinate

private val reverseGeocoderContext = newFixedThreadPoolContext(16, "reverseGeocoderContext") // TODO make parameter configurable

class ReverseGeocoderService(private val reverseQueryBuilderFactory: ReverseQueryBuilderFactory) {


    suspend fun reverseGeocode(lat: Double, lon: Double,
                               limitNumeric: Int?, limitRadius: Int?,
                               desiredLayers: List<String>?):
            Result {

        val layers = if (desiredLayers == null || desiredLayers.isEmpty()) {
            // use default layers, do not include any Relation or Junction, and VirtualTrafficFlow or PhysicalTrafficFlow
            listOf(Layer.Address, Layer.Venue, Layer.Street, Layer.Link)
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

        val closestEntity = entities.first()
        val inputGeometry = GeometryFactory().createPoint(JtsCoordinate(lon, lat))
        closestEntity.mainGeometry().value

        val distanceOp = DistanceOp(closestEntity.geometryAsJts(), inputGeometry)
        val closestPoint = distanceOp.nearestPoints().first()
        val order = entities.map { it.id }

        return Result(closestPoint, order, entities)
    }

    data class Result(
            val closestPoint: JtsCoordinate,
            val order: List<Long>,
            val entities: List<OsmEntity>
    )

}