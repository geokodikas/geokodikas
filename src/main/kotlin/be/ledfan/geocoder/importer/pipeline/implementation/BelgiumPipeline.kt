package be.ledfan.geocoder.importer.pipeline.implementation

import be.ledfan.geocoder.importer.pipeline.AbstractPipeline
import be.ledfan.geocoder.importer.pipeline.IntegrationConfig

class BelgiumPipeline : AbstractPipeline(
        IntegrationConfig("http://download.openstreetmap.fr/extracts/europe/belgium.osm.pbf",
                "belgium.osm.pbf",
                "a02f17ea9a4bd5d60d0a86bfe304faf1")
) {

}