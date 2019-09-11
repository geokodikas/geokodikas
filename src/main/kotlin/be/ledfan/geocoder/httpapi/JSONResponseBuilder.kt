package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.*
import be.ledfan.geocoder.geo.toGeoJson
import be.ledfan.geocoder.geocoding.ReverseGeocodeRequest
import be.ledfan.geojsondsl.Feature
import be.ledfan.geojsondsl.featureCollection

class JSONResponseBuilder {

    private val featureCollection = featureCollection {
        // Identical way of attribution as in nominatim
        withForeignMember("license", "Data © OpenStreetMap contributors, ODbL 1.0. https://osm.org/copyright")
    }

    fun addEntity(reverseGeocodeRequest: ReverseGeocodeRequest, osmEntity: OsmEntity, block: Feature.() -> Unit = {}) {
        addEntity(osmEntity, reverseGeocodeRequest.includeGeometry, reverseGeocodeRequest.includeTags, block)
    }

    /**
     * Order matters!
     */
    fun addEntity(osmEntity: OsmEntity, includeGeometry: Boolean = true, includeTags: List<String>? = null, block: Feature.() -> Unit = {}) {
        featureCollection.apply {
            withFeature {
                withId("feature-${osmEntity.id}")

                when (osmEntity) {
                    is OsmWay -> {
                        withProperty("osm_type", "way")
                        if (includeGeometry) {
                            withGeometry {
                                osmEntity.geometry.toGeoJson(this)
                            }
                        }
                    }
                    is OsmNode -> {
                        withProperty("osm_type", "node")
                        if (includeGeometry) {
                            withGeometry {
                                osmEntity.centroid.toGeoJson(this)
                            }
                        }
                    }
                    is OsmRelation -> {
                        withProperty("osm_type", "relation")
                        if (includeGeometry) {
                            withGeometry {
                                osmEntity.geometry.toGeoJson(this)
                            }
                        }
                    }
                    is AddressIndex -> {
                        withProperty("osm_type", "address")
                        if (includeGeometry) {
                            withGeometry {
                                osmEntity.geometry.toGeoJson(this)
                            }
                        }
                    }
                    else -> {
                        withProperty("osm_type", "unknown")
                    }
                }

                withProperty("osm_id", osmEntity.id)
                withProperty("layer", osmEntity.layer)

                if (includeTags != null) {
                    val tagData = HashMap<String, String?>()
                    for (tag in includeTags) {
                        tagData[tag] = osmEntity.tags[tag]
                    }
                    withProperty("tags", tagData)
                }

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