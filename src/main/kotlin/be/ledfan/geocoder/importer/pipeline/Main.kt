package be.ledfan.geocoder.importer.pipeline

import be.ledfan.geocoder.startup
import kotlin.system.exitProcess


suspend fun main(args: Array<String>) = startup(args, 3, "country, location of pbf file, md5sum of the file") { _ ->

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