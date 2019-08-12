package be.ledfan.geocoder.importer.pipeline.implementation


import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.pipeline.AbstractPipeline
import be.ledfan.geocoder.importer.pipeline.IntegrationConfig
import be.ledfan.geocoder.importer.steps.countTable
import org.intellij.lang.annotations.Language
import org.junit.Test


class BelgiumPipeline : AbstractPipeline(
        IntegrationConfig("http://download.openstreetmap.fr/extracts/europe/belgium.osm.pbf",
                "belgium.osm.pbf",
                "434e6864fab2ad77007e3e743a81a809")
) {


}