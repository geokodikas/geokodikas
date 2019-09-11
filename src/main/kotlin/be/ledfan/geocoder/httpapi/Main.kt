package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.config.ConfigReader
import be.ledfan.geocoder.db.ConnectionFactory
import be.ledfan.geocoder.db.mapper.ImportFromExportMetadataMapper
import be.ledfan.geocoder.importer.import_from_export.importFromExport
import ch.qos.logback.classic.util.ContextInitializer
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.freemarker.FreeMarker
import io.ktor.jackson.jackson
import io.ktor.locations.Locations
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import org.kodein.di.Instance
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.jvmType
import org.kodein.di.ktor.kodein
import org.slf4j.event.Level.*
import kotlin.system.exitProcess
import be.ledfan.geocoder.kodein as DiContainer

fun main() {
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.rest.xml");

    val logger = KotlinLogging.logger {}
    val config = ConfigReader.getConfig()

    val mb = 1024 * 1024
    val runtime = Runtime.getRuntime()
    logger.info { "Currently allocated memory (runtime.totalMemory()) " + runtime.totalMemory() / mb }
    logger.info { "Maximum allocatable memory (runtime.maxMemory()) " + runtime.maxMemory() / mb }

    if (config.importFromExport.tryImportOnHttp) {
        val metadataMapper = ImportFromExportMetadataMapper(ConnectionFactory.createWrappedConnection(config))
        // check whether import was already performed
        val last = metadataMapper.getLastImport()
        val mustImport = when {
            last == null -> {
                logger.info { "No import performed yet, going to import with sum ${config.importFromExport.fileMd5Sum}" }
                true
            }
            last.md5sum == config.importFromExport.fileMd5Sum -> {
                logger.info { "Already has import with sum ${last.md5sum}." }
                false
            }
            else -> {
                logger.info { "Already has import, but with different sum ${last.md5sum}. Database must be clean before importing. Exiting." }
                exitProcess(1)
            }
        }

        if (mustImport) {
            try {
                importFromExport()
                logger.info { "Import performed, exiting." }
                metadataMapper.insert(config.importFromExport.fileMd5Sum, config.importFromExport.fileLocation)
                exitProcess(0)
            } catch (e: Exception) {
                logger.error(e) { "Exception during import"}
                exitProcess(1)
            }
        }
    }

    embeddedServer(Netty,
            port = 8080,
            watchPaths = listOf("be/ledfan"),
            module = Application::kodeinApplication)
            .start(wait = true)
}


/**
 * Registers a [kodeinApplication] that that will call [kodeinMapper] for mapping stuff.
 * The [kodeinMapper] is a lambda that is in charge of mapping all the required.
 *
 * After calling [kodeinMapper], this function will search
 * for registered subclasses of [KodeinController], and will call their [KodeinController.registerRoutes] methods.
 */
fun Application.kodeinApplication() {
    val application = this

    kodein {
        DiContainer.addConfig {
            bind<Application>() with instance(application)
        }
    }

    application.install(Locations)
    application.install(CallLogging) {
        level = INFO
    }
    application.install(ContentNegotiation) { jackson {} }
    application.install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }


    /**
     * Detects all the registered [KodeinController] and registers its routes.
     */
    routing {
        for (bind in DiContainer.container.tree.bindings) {
            val bindClass = bind.key.type.jvmType as? Class<*>?
            if (bindClass != null && KodeinController::class.java.isAssignableFrom(bindClass)) {
                val res by DiContainer.Instance(bind.key.type)
                (res as KodeinController).apply { registerRoutes() }
            }
        }
        trace { application.log.trace(it.buildText()) }
    }
}

