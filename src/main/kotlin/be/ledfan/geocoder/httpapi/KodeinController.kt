package be.ledfan.geocoder.httpapi

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.locations.locations
import io.ktor.response.respond
import io.ktor.routing.Routing
import mu.KotlinLogging
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance

/**
 * A [KodeinAware] base class for Controllers handling routes.
 * It allows to easily get dependencies, and offers some useful extensions like getting the [href] of a [TypedRoute].
 */
abstract class KodeinController(override val kodein: Kodein) : KodeinAware {
    /**
     * Injected dependency with the current [Application].
     */
    protected val application: Application by instance()

    /**
     * Shortcut to get the url of a [TypedRoute].
     */
    protected val TypedRoute.href get() = application.locations.href(this)

    /**
     * Method that subtypes must override to register the handled [Routing] routes.
     */
    abstract fun Routing.registerRoutes()

    private val logger = KotlinLogging.logger {}

    protected suspend fun withErrorHandling(call: ApplicationCall, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: HttpApiBadRequestException) {
            // this exception is a "normal" error for the API
            // return the provided message to the user, without logging a stack trace
            call.respond(BadRequest, mapOf("status" to "bad_request", "msg" to (e.message ?: "Unknown error occurred")))
        } catch (e: HttpApiNotFoundException) {
            // this exception is a "normal" error for the API
            // return the provided message to the user, without logging a stack trace
            call.respond(NotFound, mapOf("status" to "not_found", "msg" to (e.message ?: "Unknown error occurred")))
        } catch (e: HttpApiErrorException) {
            // this exception is a "normal" error for the API
            // return the provided message to the user, without logging a stack trace
            call.respondError(e.message ?: "Unknown error occurred")
        } catch (e: Exception) {
            logger.error(e) { "Exception during processing of HTTP Request" }
            call.respondError("Unknown exception occurred")
        } catch (e: Error) {
            logger.error(e) { "Error during processing of HTTP Request" }
            call.respondError("Unknown error occurred")
        }
    }

}