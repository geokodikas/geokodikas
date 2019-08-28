package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.kodein
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.util.*
import kotlin.collections.ArrayList

private val reverseGeocoderContext = newFixedThreadPoolContext(16, "reverseGeocoderContext") // TODO make parameter configurable

class ReverseGeocoderService {

    private val reverseQueryBuilderFactory = ReverseQueryBuilderFactory()

    suspend fun reverseGeocode(lat: Double, lon: Double,
                               limitNumeric: Int?, limitRadius: Int?,
                               desiredLayers: List<String>?):
            Triple<List<OsmNode>, List<OsmWay>, List<OsmRelation>> {

        val layers = if (desiredLayers == null || desiredLayers.isEmpty()) {
            Layer.values().toList()
        } else {
            desiredLayers.map { Layer.valueOf(it) }
        }

        val requiredTables = getTablesForLayers(layers)

        val nodes = Collections.synchronizedList(ArrayList<OsmNode>())
        val ways = Collections.synchronizedList(ArrayList<OsmWay>())
        val relations = Collections.synchronizedList(ArrayList<OsmRelation>())
        val addresses = Collections.synchronizedList(ArrayList<AddressIndex>())

        withContext(reverseGeocoderContext) {
            for (table in requiredTables) {
                this@withContext.launch {
                    // query and process each table in parallel
                    val privateCon = kodein.direct.instance<ConnectionWrapper>()
                    val (sqlQuery, parameters) = reverseQueryBuilderFactory.createBuilder(table, debug = true).run {
                        baseQuery(lat, lon, requiredTables)
                        if (limitRadius != null && limitRadius > 0) {
                            whereMetricDistance(limitRadius)
                        }
                        if (desiredLayers != null) {
                            whereLayer(layers)
                        }
                        orderBy()
                        if (limitNumeric != null && limitNumeric > 0) {
                            limit(limitNumeric)
                        } else {
                            limit(5)
                        }
                        build()
                    }

                    val stmt = privateCon.prepareStatement(sqlQuery)

                    parameters.forEachIndexed { index, param ->
                        stmt.setObject(index + 1, param)
                    }

                    val result = stmt.executeQuery()
                    while (result.next()) {
                        reverseQueryBuilderFactory.processResult(table, result, nodes, ways, relations, addresses)
                    }

                    stmt.close()
                    result.close()
                }
            }
        }

        return Triple(nodes.toList(), ways.toList(), relations.toList())
    }

}