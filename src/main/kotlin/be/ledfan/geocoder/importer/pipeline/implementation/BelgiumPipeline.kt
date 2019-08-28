package be.ledfan.geocoder.importer.pipeline.implementation

import be.ledfan.geocoder.importer.pipeline.AbstractPipeline
import be.ledfan.geocoder.importer.pipeline.IntegrationConfig
import kotlin.contracts.ExperimentalContracts

class BelgiumPipeline : AbstractPipeline(
        IntegrationConfig("http://download.openstreetmap.fr/extracts/europe/belgium.osm.pbf",
                "belgium.osm.pbf",
                "0c1d93be05ce2ce87a59b65bda77767d")
) {

    @ExperimentalContracts
    override fun validate(): Boolean {
        val validator = BelgiumValidator()
        return validator.validate()
    }

}