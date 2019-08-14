package be.ledfan.geocoder.importer.pipeline.implementation

import be.ledfan.geocoder.importer.pipeline.AbstractPipeline
import be.ledfan.geocoder.importer.pipeline.IntegrationConfig

class BelgiumPipeline : AbstractPipeline(
        IntegrationConfig("http://download.openstreetmap.fr/extracts/europe/belgium.osm.pbf",
                "belgium.osm.pbf",
                "49700d24ff9754bc491806feeb539d38")
) {


}