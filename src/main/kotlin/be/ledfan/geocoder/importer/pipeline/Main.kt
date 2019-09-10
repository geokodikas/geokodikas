package be.ledfan.geocoder.importer.pipeline

import ch.qos.logback.classic.util.ContextInitializer
import kotlin.system.exitProcess


fun main(args: Array<String>) {

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.pipeline.xml");

    if (args.size != 3) {
        println("Must provide three parameters: country, location of pbf file and md5sum of the file.")
        exitProcess(1)
    }

    val countryName = args[0]
    val pbfUrl = args[1]
    val md5sum = args[2]

    val pipelineFactory = pipelines[countryName]

    if (pipelineFactory == null) {
        println("No pipeline found for country $countryName, available pipelines: ${pipelines.keys.joinToString()}")
        exitProcess(1)
    }

    val pipeline = pipelineFactory(IntegrationConfig(pbfUrl, countryName, md5sum))
    pipeline.import()

    if (pipeline.validate()) {
        pipeline.dropUpstreamTables()
        pipeline.export()
    } else {
        println("Validation errors, not exporting")
        exitProcess(2)
    }

}