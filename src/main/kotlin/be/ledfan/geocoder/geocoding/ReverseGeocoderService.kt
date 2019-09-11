package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.importer.Layer
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.operation.distance.DistanceOp
import kotlin.collections.ArrayList
import kotlin.math.min
import org.locationtech.jts.geom.Coordinate as JtsCoordinate

class ReverseGeocoderService(private val reverseQueryBuilderFactory: ReverseQueryBuilderFactory,
                             private val humanAddressBuilderService: HumanAddressBuilderService) {

    private val reverseGeocoderContext = newFixedThreadPoolContext(16, "reverseGeocoderContext") // TODO make parameter configurable

    suspend fun reverseGeocode(lat: Double, lon: Double,
                               limitNumeric: Int?, limitRadius: Int?,
                               desiredLayers: List<Layer>?): Result {

        val layers = if (desiredLayers == null || desiredLayers.isEmpty()) {
            // use default layers, do not include any Relation or Junction, and VirtualTrafficFlow or PhysicalTrafficFlow
            listOf(Layer.Address, Layer.Venue, Layer.Street, Layer.Link)
        } else {
            desiredLayers
        }

        val requiredTables = getTablesForLayers(layers)

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


        val deferredEntities = withContext(reverseGeocoderContext) {
            requiredTables.map { table ->
                async(reverseGeocoderContext) {
                    reverseQueryBuilderFactory.createBuilder(table, humanAddressBuilderService).run {
                        setupArgs(lat, lon, actualLimitRadius, desiredLayers != null)
                        initQuery()
                        if (desiredLayers != null) {
                            whereLayer(layers)
                        }
                        orderBy()
                        limit(actualLimitNumeric)
                        val r = ArrayList<OsmEntity>()
                        execute(r)
                        println(buildQueryForDebugging())
                        r
                    }
                }
            }
        }

        var entities = ArrayList(deferredEntities.map { it.await() }.flatten())

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