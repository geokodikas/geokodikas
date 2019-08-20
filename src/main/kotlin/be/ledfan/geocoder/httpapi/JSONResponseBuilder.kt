package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.geo.toGeoJson
import be.ledfan.geojsondsl.Feature
import be.ledfan.geojsondsl.feature
import be.ledfan.geojsondsl.featureCollection
import com.beust.klaxon.JsonObject

class JSONResponseBuilder {

    //    private val osmEntities = ArrayList<OsmEntity>()
    private val featureCollection = featureCollection()

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
                    else -> {
                        withProperty("osm_type", "unknown")
                    }
                }

                withProperty("osm_id", osmEntity.id)

                block()
            }
        }
    }

    fun toJson() = featureCollection.toJson()


}