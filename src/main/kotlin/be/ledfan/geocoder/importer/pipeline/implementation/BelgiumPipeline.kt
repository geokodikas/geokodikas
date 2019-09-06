package be.ledfan.geocoder.importer.pipeline.implementation

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.importer.pipeline.AbstractPipeline
import be.ledfan.geocoder.importer.pipeline.IntegrationConfig
import be.ledfan.geocoder.kodein
import org.kodein.di.direct
import org.kodein.di.generic.instance
import kotlin.contracts.ExperimentalContracts

class BelgiumPipeline(ic: IntegrationConfig) : AbstractPipeline(ic) {

    init {
        val config = kodein.direct.instance<Config>()
        config.importer.countryId = 52411L
    }

    @ExperimentalContracts
    override fun validate(): Boolean {
        val validator = BelgiumValidator()
        return validator.validate()
    }

}