package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.db.entity.*

class ReverseQueryBuilderFactory() {

    fun createBuilder(osmType: OsmType, humanAddressBuilderService: HumanAddressBuilderService): ReverseQueryBuilder {
        return when (osmType) {
            OsmType.Node -> {
                NodeReverseQueryBuilder(humanAddressBuilderService)
            }
            OsmType.Way -> {
                WayReverseQueryBuilder(humanAddressBuilderService)
            }
            OsmType.Relation -> {
                RelationReverseQueryBuilder(humanAddressBuilderService)
            }
            OsmType.AddressIndex -> {
                AddressIndexReverseQueryBuilder(humanAddressBuilderService)
            }
        }
    }

}