package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.*
import be.ledfan.geocoder.geo.toGeoJson
import be.ledfan.geojsondsl.Feature
import be.ledfan.geojsondsl.feature
import be.ledfan.geojsondsl.featureCollection
import com.beust.klaxon.JsonObject

class JSONResponseBuilder {

    private val featureCollection = featureCollection {
        // Identical way of attribution as in nominatim
        withForeignMember("license", "Data © OpenStreetMap contributors, ODbL 1.0. https://osm.org/copyright")
    }

    /**
     * Order matters!
     */
    fun addEntity(osmEntity: OsmEntity, block: Feature.() -> Unit = {}) {
        featureCollection.apply {
            withFeature {
                withId("feature-${osmEntity.id}")

                when (osmEntity) {
                    is OsmWay -> {
                        withProperty("osm_type", "way")
                        withGeometry {
                            osmEntity.geometry.toGeoJson(this)
                        }
                    }
                    is OsmNode -> {
                        withProperty("osm_type", "node")
                        withGeometry {
                            osmEntity.centroid.toGeoJson(this)
                        }
                    }
                    is OsmRelation -> {
                        withProperty("osm_type", "relation")
                        withGeometry {
                            osmEntity.geometry.toGeoJson(this)
                        }
                    }
                    is AddressIndex -> {
                        withProperty("osm_type", "address")
                        withGeometry {
                            osmEntity.geometry.toGeoJson(this)
                        }
                    }
                    else -> {
                        withProperty("osm_type", "unknown")
                    }
                }

                withProperty("osm_id", osmEntity.id)

                if (osmEntity.dynamicProperties.size > 0) {
                    withProperty("dynamic_properties", osmEntity.dynamicProperties)
                }

                block()
            }
        }
    }

    fun toJson() = featureCollection.toJson()

    fun addFeature(block: Feature.() -> Unit = {}) {
        featureCollection.apply {
            withFeature {
                block()
            }
        }
    }

}