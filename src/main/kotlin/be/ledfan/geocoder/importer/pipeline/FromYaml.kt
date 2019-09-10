package be.ledfan.geocoder.importer.pipeline

import be.ledfan.geocoder.importer.pipeline.implementation.BelgiumPipeline
import be.ledfan.geocoder.importer.pipeline.implementation.MonacoPipeline
import ch.qos.logback.classic.util.ContextInitializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mu.KotlinLogging


data class Export(val completed: Boolean,
                  val download_date: String,
                  val osm_pbf_file: String,
                  val osm_pbf_md5sum: String,
                  val export_download_url: String,
                  val export_filename: String,
                  val export_md5sum: String
)

data class Countries(var countries: Map<String, List<Export>>)

val pipelines = HashMap<String, (IntegrationConfig) -> AbstractPipeline>().also {
    it["Belgium"] = { ic -> BelgiumPipeline(ic) }
    it["Monaco"] = { ic -> MonacoPipeline(ic) }
}

fun main() {

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.pipeline.xml");
    val logger = KotlinLogging.logger {}
    val mb = 1024 * 1024
    val runtime = Runtime.getRuntime()
    logger.info { "Currently allocated memory (runtime.totalMemory()) " + runtime.totalMemory() / mb }
    logger.info { "Maximum allocatable memory (runtime.maxMemory()) " + runtime.maxMemory() / mb }

    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support

    val countries = mapper.readValue(System.`in`, Countries::class.java)

    for ((countryName, exports) in countries.countries) {
        for (export in exports) {
            if (!export.completed) {
                val pipelineFactory = pipelines[countryName] ?: TODO()
                val pipeline = pipelineFactory(IntegrationConfig(export.osm_pbf_file, countryName, export.osm_pbf_md5sum))

                pipeline.import()
                if (pipeline.validate()) {
                    pipeline.dropUpstreamTables()
                    val (exportFileName, md5sum) = pipeline.export()
                    println("Export filename: $exportFileName")
                    println("Export md5sum: $md5sum")
                } else {
                    println("Validation errors, not exporting")
                }
            }
        }
    }
}