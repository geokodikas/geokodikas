package be.ledfan.geocoder.importer.pipeline.implementation

import be.ledfan.geocoder.importer.pipeline.AbstractPipeline
import be.ledfan.geocoder.importer.pipeline.IntegrationConfig

// Bogus pipeline for fast testing
class MonacoPipeline : AbstractPipeline(
        IntegrationConfig("http://download.openstreetmap.fr/extracts/europe/monaco.osm.pbf",
                "monaco.osm.pbf",
                "eacf941f8acbd0ca68bb98b02e2e6b98")
) {

    override fun validate(): Boolean {
        val validator = MonacoValidator()
        return validator.validate()
    }

}
