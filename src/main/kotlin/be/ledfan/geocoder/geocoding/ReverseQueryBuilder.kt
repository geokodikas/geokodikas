package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.addresses.LangCode
import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.forFirstAndRest
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.kodein
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.sql.ResultSet
import kotlin.math.roundToInt

abstract class ReverseQueryBuilder(protected val humanAddressBuilderService: HumanAddressBuilderService) {

    // Internal state
    private var hasWhere = false
    var currentQuery = ""
        protected set

    var parameters = ArrayList<Any>()
        protected set

    protected var lat: Double = 0.0
    protected var lon: Double = 0.0
    protected var metricDistance: Int = 0
    protected var hasLayerLimits: Boolean = false

    abstract fun initQuery()
    abstract fun processResult(result: ResultSet): OsmEntity


    fun setupArgs(lat: Double, lon: Double, metricDistance: Int, hasLayerLimits: Boolean) {
        this.lat = lat
        this.lon = lon
        this.metricDistance = metricDistance
        this.hasLayerLimits = hasLayerLimits
    }

    fun orderBy(): ReverseQueryBuilder {
        val sql = """
            ORDER BY metric_distance
            """
        currentQuery += sql
        return this
    }

    fun buildQueryForDebugging(): String {
        var filledQuery = currentQuery
        parameters.forEach { parameterValue ->
            filledQuery = filledQuery.replaceFirst("?", parameterValue.toString())
        }
        return filledQuery
    }

    protected fun withWhere(where: String) {
        currentQuery += if (!hasWhere) {
            hasWhere = true
            """
            WHERE $where
            """
        } else {
            """
            AND $where
            """
        }
    }

    fun limit(limit: Int): ReverseQueryBuilder {
        currentQuery += "LIMIT ?"
        parameters.add(limit)
        return this
    }

    fun whereLayer(layers: List<Layer>): ReverseQueryBuilder {
        if (!hasLayerLimits) {
            throw Exception("HasLayerLimits is false, cannot specify layer to limit on")
        }
        if (layers.isEmpty()) return this

        var whereClause = "("
        layers.forFirstAndRest({ layer ->
            whereClause += "layer = ?::Layer "
            parameters.add(layer.toString())
        }, { layer ->
            whereClause += "OR layer = ?::Layer "
            parameters.add(layer.toString())
        })
        whereClause += ")"

        withWhere(whereClause)
        return this
    }

    fun execute(entities: MutableList<OsmEntity>) {
        val privateCon = kodein.direct.instance<ConnectionWrapper>()
        val stmt = privateCon.prepareStatement(currentQuery)

        parameters.forEachIndexed { index, param ->
            stmt.setObject(index + 1, param)
        }

        val result = stmt.executeQuery()

        while (result.next()) {
            entities.add(processResult(result))
        }

        stmt.close()
        result.close()
    }

    fun processEntity(entity: OsmEntity, row: ResultSet): OsmEntity {
        entity.dynamicProperties["distance"] = row.getDouble("metric_distance").roundToInt()
        humanAddressBuilderService.nameOfEntity(LangCode.NL, entity)?.let { name ->
            entity.dynamicProperties["name"] = name
        }
        return entity
    }

}