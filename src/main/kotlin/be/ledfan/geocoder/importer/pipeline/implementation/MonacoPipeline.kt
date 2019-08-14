package be.ledfan.geocoder.importer.pipeline.implementation

import be.ledfan.geocoder.importer.pipeline.AbstractPipeline
import be.ledfan.geocoder.importer.pipeline.IntegrationConfig

// Bogus pipeline for fast testing
class MonacoPipeline : AbstractPipeline(
        IntegrationConfig("http://download.openstreetmap.fr/extracts/europe/monaco.osm.pbf",
                "monaco.osm.pbf",
                "e74914f076d0c5a2e36302bfee1a01e2")
) {

}