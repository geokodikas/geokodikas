@file:Suppress("FunctionName")

package be.ledfan.geocoder.importer.core

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.importer.steps.*
import be.ledfan.geocoder.kodein
import mu.KotlinLogging
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.lang.Exception
import kotlin.system.exitProcess

class Importer {

    private val logger = KotlinLogging.logger {}

    private val config: Config = kodein.direct.instance()

    private val steps = HashMap<String, List<Pair<String, suspend () -> Boolean>>>()

    fun setup(inputFileName: String) {

        kodein.direct.instance<StatsCollector>().startShowingSpinner()

        config.runtime.inputFileName = inputFileName

        logger.info { "Starting import of $inputFileName" }
        logger.info { "Bootstrapping importer" }
        setMaximumThreads()
    }

    init {
        steps["drop_tables"] = listOf(Pair("drop_tables", ::drop_tables))

        steps["step0"] = listOf(
                Pair("step0_checks", ::step0_checks),
                Pair("step0_create_schema", ::step0_create_schema),
                Pair("step0_create_indexes", ::step0_create_indexes))

        steps["step1"] = listOf(
                Pair("step1_checks", ::step1_checks),
                Pair("step1_import_ways", ::step1_import_ways),
                Pair("step1_create_indexes", ::step1_create_indexes))

        steps["step2"] = listOf(
                Pair("step2_checks", ::step2_checks),
                Pair("step2_import_nodes", ::step2_import_nodes),
                Pair("step2_create_indexes", ::step2_create_indexes),
//                Pair("step2_resolve_distances_way_node", ::step2_resolve_distances_way_node),
                Pair("step2_prune_nodes_without_layer", ::step2_prune_nodes_without_layer))
//                Pair("step2_resolve_one_ways", ::step2_resolve_one_ways))

        steps["step3"] = listOf(
                Pair("step3_checks", ::step3_checks),
                Pair("step3_import_relations", ::step3_import_relations),
                Pair("step3_set_centroid", ::step3_set_centroid),
                Pair("step3_create_indexes", ::step3_create_indexes),
                Pair("step3_prune_relations", ::step3_prune_relations),
                Pair("step3_resolve_hierarchies", ::step3_resolve_hierarchies))
    }


    suspend fun executeStep(name: String) {
        val commandsToExecute = steps[name] ?: return stopWithFatalError("Step $name not found")

        for (command in commandsToExecute) {
            logger.info { "Starting function ${command.first} of step $name" }
            try {
                val ret = command.second()
                if (!ret) {
                    return stopWithFatalError("Error during execution of step $name, function: ${command.first}")
                } else {
                    logger.info { "Executed function ${command.first} of step $name" }
                }
            } catch (e: Exception) {
                return stopWithFatalError(e)
            }
        }
    }

    fun finish() {
        kodein.direct.instance<StatsCollector>().finish()
        println()
        println("Import results")

        for (table in listOf("osm_node", "osm_relation", "osm_way", "parent", "way_node")) {
            println("$table has ${countTable(table)} records")
        }

        println()
    }

    fun stopWithFatalError(message: String) {
        synchronized(this) {
            // execute the stop procedure only once
            kodein.direct.instance<StatsCollector>().finish()

            logger.error { "Stopped due to fatal error. Message: $message" }

            println()
            println("Stopped due to fatal error. Message: $message")
            exitProcess(1)
        }
    }

    fun stopWithFatalError(cause: Exception) {
        synchronized(this) {
            // execute the stop procedure only once
            kodein.direct.instance<StatsCollector>().finish()

            println()
            logger.error(cause) { "Stopped due to fatal error." }
            println("Stopped due to fatal error. Cause:")
            cause.printStackTrace()
            exitProcess(1)
        }
    }

    /**
     * Depending on the configuration set the amount of threads that can be used by Kotlin for the IO coroutines
     */
    private fun setMaximumThreads() {

        val maximumThreads = config.importer.numProcessors + 10

        if (maximumThreads > 64) {
            // 64 is default by Kotlin

            val availableProcessors = Runtime.getRuntime().availableProcessors()

            if (maximumThreads > availableProcessors) {
                logger.warn { "Using more threads ($maximumThreads) than available processors ($availableProcessors)" }
            }

            System.setProperty(kotlinx.coroutines.IO_PARALLELISM_PROPERTY_NAME, "512")
        }

    }

}
