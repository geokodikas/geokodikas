package be.ledfan.geocoder.importer

import be.ledfan.geocoder.importer.core.Importer
import be.ledfan.geocoder.kodein
import ch.qos.logback.classic.util.ContextInitializer
import org.kodein.di.direct
import org.kodein.di.generic.instance

suspend fun main(args: Array<String>) {


    if (args.isEmpty()) {
        println("Not enough arguments, exiting.")
        return
    }

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.importer.xml");

    val importer = kodein.direct.instance<Importer>()

    if (args[0] == "--drop-tables") {
        importer.executeStep("drop_tables")
        return
    }

    importer.run {
        setup(args[0])

        executeStep("step0")
        executeStep("step1")
        executeStep("step2")
        executeStep("step3")
        executeStep("step4")

        finish()
    }
    println()

}
