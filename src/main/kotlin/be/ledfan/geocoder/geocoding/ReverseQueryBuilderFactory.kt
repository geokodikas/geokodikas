package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.addresses.LangCode
import be.ledfan.geocoder.db.entity.*
import java.sql.ResultSet
import kotlin.math.roundToInt

class ReverseQueryBuilderFactory(private val humanAddressBuilderService: HumanAddressBuilderService) {

    fun createBuilder(table: OsmType, debug: Boolean = false): ReverseQueryBuilder {
        return when (table) {
            OsmType.Node -> {
                NodeReverseQueryBuilder(debug)
            }
            OsmType.Way -> {
                WayReverseQueryBuilder(debug)
            }
            OsmType.Relation -> {
                TODO()
            }
            OsmType.AddressIndex -> {
                AddressIndexReverseQueryBuilder(debug)
            }
        }
    }

    fun processResult(table: OsmType, row: ResultSet,
                      entities: MutableList<OsmEntity>) {

        fun commonProcess(entity: OsmEntity) {
            entity.dynamicProperties["distance"] = row.getDouble("metric_distance").roundToInt()
            humanAddressBuilderService.nameOfEntity(LangCode.NL, entity)?.let { name ->
                entity.dynamicProperties["name"] = name
            }

            entities.add(entity)
        }

        when (table) {
            OsmType.Node -> {
                val node = OsmNode.fillFromRow(row)
                commonProcess(node)
            }
            OsmType.Way -> {
                val way = OsmWay.fillFromRow(row)
                commonProcess(way)
            }
            OsmType.Relation -> {
                val relation = OsmRelation.fillFromRow(row)
                commonProcess(relation)
            }
            OsmType.AddressIndex -> {
                val addressIndex = AddressIndex.fillFromRow(row)
                val address = humanAddressBuilderService.build(LangCode.NL, addressIndex)
                addressIndex.dynamicProperties["Address"] = address
                commonProcess(addressIndex)
            }
        }

    }

}