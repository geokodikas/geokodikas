package be.ledfan.geocoder.importer

import be.ledfan.geocoder.importer.core.Importer
import be.ledfan.geocoder.kodein
import org.kodein.di.direct
import org.kodein.di.generic.instance

suspend fun main(args: Array<String>) {

    if (args.isEmpty()) {
        println("Not enough arguments, exiting.")
        return
    }

    val importer = kodein.direct.instance<Importer>()
    importer.run {
        setup(args[0])

        executeStep("step0")
        executeStep("step1")
//        executeStep("step2")
//        executeStep("step3")

        finish()
    }
    println()

}
