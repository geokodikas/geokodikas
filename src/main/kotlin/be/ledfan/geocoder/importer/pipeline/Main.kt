package be.ledfan.geocoder.importer.pipeline

import be.ledfan.geocoder.importer.pipeline.implementation.BelgiumPipeline
import be.ledfan.geocoder.importer.pipeline.implementation.MonacoPipeline
import ch.qos.logback.classic.util.ContextInitializer
import kotlin.system.exitProcess


fun main(args: Array<String>) {

    val pipelines = HashMap<String, () -> AbstractPipeline>()
    pipelines["Belgium"] = { BelgiumPipeline() }
    pipelines["Monaco"] = { MonacoPipeline() }

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.pipeline.xml");

    if (args.size != 1) {
        println("Must provide exactly one parameter: the country to import.")
        exitProcess(1)
    }

    val countryName = args[0]


    val pipelineFactory = pipelines[countryName]

    if (pipelineFactory == null) {
        println("No pipeline found for country $countryName, available pipelines: ${pipelines.keys.joinToString()}")
        exitProcess(1)
    }

    val pipeline = pipelineFactory()
    pipeline.import()
//    pipeline.validate<BelgiumValidator>()
    pipeline.dropUpstreamTables()
    pipeline.export()

}