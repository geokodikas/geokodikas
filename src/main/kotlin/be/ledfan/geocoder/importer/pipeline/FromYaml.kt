package be.ledfan.geocoder.importer.pipeline

import be.ledfan.geocoder.importer.pipeline.implementation.BelgiumPipeline
import be.ledfan.geocoder.importer.pipeline.implementation.MonacoPipeline
import ch.qos.logback.classic.util.ContextInitializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule


data class Export(val completed: Boolean,
                  val download_date: String,
                  val osm_pbf_file: String,
                  val osm_pbf_md5sum: String,
                  val export_download_url: String,
                  val export_filename: String,
                  val export_md5sum: String
)

data class Countries(var countries: Map<String, List<Export>>)

fun main() {

    val pipelines = HashMap<String, (IntegrationConfig) -> AbstractPipeline>()
    pipelines["Belgium"] = { ic -> BelgiumPipeline(ic) }
    pipelines["Monaco"] = { ic -> MonacoPipeline(ic) }

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.pipeline.xml");

    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support

    val countries = mapper.readValue(System.`in`, Countries::class.java)

    for ((countryName, exports) in countries.countries) {
        for (export in exports) {
            if (!export.completed) {
                val pipelineFactory = pipelines[countryName] ?: TODO()
                val pipeline = pipelineFactory(IntegrationConfig(export.osm_pbf_file,
                        "${countryName}_${export.osm_pbf_md5sum}",
                        export.osm_pbf_md5sum))

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