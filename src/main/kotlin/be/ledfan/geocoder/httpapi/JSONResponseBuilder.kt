package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.geo.toGeoJson
import be.ledfan.geojsondsl.feature
import be.ledfan.geojsondsl.featureCollection
import com.beust.klaxon.JsonObject

class JSONResponseBuilder {

    private val osmEntities = ArrayList<OsmEntity>()

    /**
     * Order matters!
     */
    fun addEntity(osmEntity: OsmEntity) {
        if (osmEntities.contains(osmEntity)) {
            return
        }
        osmEntities.add(osmEntity)
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
                }
            }
        }.toJson()
    }


}