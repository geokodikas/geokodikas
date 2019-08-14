package be.ledfan.geocoder.importer.pipeline

import be.ledfan.geocoder.importer.pipeline.implementation.BelgiumPipeline
import be.ledfan.geocoder.importer.pipeline.implementation.BelgiumValidator
import be.ledfan.geocoder.importer.pipeline.implementation.MonacoPipeline
import ch.qos.logback.classic.util.ContextInitializer

fun main() {

//    val belgiumPipeline = BelgiumPipeline()
//    belgiumPipeline.import()
//    belgiumPipeline.validate<BelgiumValidator>()
//    belgiumPipeline.dropUpstreamTables()
//    belgiumPipeline.export()

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.pipeline.xml");

    val monacoPipeline = MonacoPipeline()
    monacoPipeline.import()
    monacoPipeline.validate<BelgiumValidator>()
    monacoPipeline.dropUpstreamTables()
    monacoPipeline.export()
}