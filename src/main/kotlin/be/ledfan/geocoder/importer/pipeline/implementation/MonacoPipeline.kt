package be.ledfan.geocoder.importer.pipeline.implementation

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.importer.pipeline.AbstractPipeline
import be.ledfan.geocoder.importer.pipeline.IntegrationConfig
import be.ledfan.geocoder.kodein
import org.kodein.di.direct
import org.kodein.di.generic.instance

// Bogus pipeline for fast testing
class MonacoPipeline(ic: IntegrationConfig) : AbstractPipeline(ic) {

    init {
        val config = kodein.direct.instance<Config>()
        config.importer.countryId = 36990L
    }

    override fun validate(): Boolean {
        val validator = MonacoValidator()
        return validator.validate()
    }

}
