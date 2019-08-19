package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.geo.toGeoJson
import be.ledfan.geojsondsl.feature
import be.ledfan.geojsondsl.featureCollection
import com.beust.klaxon.JsonObject

class ResponseBuilder {

    private val osmEntities = ArrayList<OsmEntity>()

    /**
     * Order matters!
     */
    fun addEntity(osmEntity: OsmEntity) {
        // TODO check for duplicate
        osmEntities.add(osmEntity)
    }


    fun buildAsSingleFeautre(): JsonObject {
        if (osmEntities.size > 1) {
            throw Exception("More than 1 feature, cannot build signle feautre")
        }

        return feature {
            osmEntities.forEachIndexed { idx, osmEntity ->
                withId("feature-$idx")

                when (osmEntity) {
                    is OsmWay -> {
                        withProperty("osm_type", "way")
                        withGeometry {
                            osmEntity.geometry.toGeoJson(this)
                        }
                    }
                    else -> {
                        withProperty("osm_type", "unknown")
                    }
                }

                withProperty("osm_id", osmEntity.id)
            }
        }.toJson()
    }

    fun buildAsCollection(): JsonObject {
        return featureCollection {
            osmEntities.forEachIndexed { idx, osmEntity ->
                withFeature {
                    withId("feature-$idx")

                    when (osmEntity) {
                        is OsmWay -> {
                            withProperty("osm_type", "way")
                            withGeometry {
                                osmEntity.geometry.toGeoJson(this)
                            }
                        }
                        else -> {
                            withProperty("osm_type", "unknown")
                        }
                    }

                    withProperty("osm_id", osmEntity.id)
                }
            }
        }.toJson()
    }


}