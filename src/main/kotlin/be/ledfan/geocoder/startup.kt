package be.ledfan.geocoder

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.config.ConfigReader
import ch.qos.logback.classic.util.ContextInitializer
import mu.KotlinLogging
import java.io.File
import kotlin.system.exitProcess

data class StartupData(val config: Config)

suspend fun startup(args: Array<String>? = null, expectedArgSize: Int? = null,
                    expectedArgHelp: String? = null, block: suspend (StartupData) -> Unit) {

    if (args != null && expectedArgSize != null && args.size != expectedArgSize) {
        println("Must provide three parameters: $expectedArgHelp")
        exitProcess(1)
    }

    if (File("logback.geokodikas.xml").exists()) {
        // if logging file exists, use that
        System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.geokodikas.xml");
    } else {
        // otherwise fall back to the file in the resources directory
        System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.xml");
    }

    val logger = KotlinLogging.logger {}
    val config = ConfigReader.getConfig()

    val mb = 1024 * 1024
    val runtime = Runtime.getRuntime()

    logger.info { "Currently allocated memory (runtime.totalMemory()) " + runtime.totalMemory() / mb }
    logger.info { "Maximum allocatable memory (runtime.maxMemory()) " + runtime.maxMemory() / mb }

    block(StartupData(config))
}