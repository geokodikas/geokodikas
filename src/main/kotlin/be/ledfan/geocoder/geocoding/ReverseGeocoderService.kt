package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.importer.Layer
import kotlinx.coroutines.async
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

    suspend fun reverseGeocode(reverseGeocodeRequest: ReverseGeocodeRequest): Result {

        val deferredEntities = withContext(reverseGeocoderContext) {
            reverseGeocodeRequest.requiredTables.map { table ->
                async(reverseGeocoderContext) {
                    reverseQueryBuilderFactory.createBuilder(table, humanAddressBuilderService).run {
                        setupArgs(reverseGeocodeRequest)
                        build()
                        execute()
                    }
                }
            }
        }

        var entities = ArrayList(deferredEntities.map { it.await() }.flatten())

        entities.sortBy { it.dynamicProperties["distance"] as Int }
        entities = ArrayList(entities.subList(0, min(reverseGeocodeRequest.limitNumeric, entities.size)))

        val closestPoint = if (reverseGeocodeRequest.includeGeometry) {
            val closestEntity = entities.first()
            val inputGeometry = GeometryFactory().createPoint(JtsCoordinate(reverseGeocodeRequest.lon, reverseGeocodeRequest.lat))
            closestEntity.mainGeometry().value
            val distanceOp = DistanceOp(closestEntity.geometryAsJts(), inputGeometry)
            distanceOp.nearestPoints().first()
        } else {
            null
        }

        val order = entities.map { it.id }

        return Result(closestPoint, order, entities)
    }

    data class Result(
            val closestPoint: org.locationtech.jts.geom.Coordinate?,
            val order: List<Long>,
            val entities: List<OsmEntity>
    )

}