package be.ledfan.geocoder.httpapi

import ch.qos.logback.classic.util.ContextInitializer
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.freemarker.FreeMarker
import io.ktor.jackson.jackson
import io.ktor.locations.Locations
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.kodein.di.Instance
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.jvmType
import org.kodein.di.ktor.kodein
import be.ledfan.geocoder.kodein as DiContainer

fun main() {
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.rest.xml");
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
    application.install(CallLogging)
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
    }
}

