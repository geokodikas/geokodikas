package be.ledfan.geocoder.importer.pipeline

import be.ledfan.geocoder.importer.pipeline.implementation.BelgiumPipeline
import be.ledfan.geocoder.importer.pipeline.implementation.BelgiumValidator

fun main() {

    val belgiumPipeline = BelgiumPipeline()
    belgiumPipeline.import()
    belgiumPipeline.validate<BelgiumValidator>()
    belgiumPipeline.dropUpstreamTables()
    belgiumPipeline.export()

}