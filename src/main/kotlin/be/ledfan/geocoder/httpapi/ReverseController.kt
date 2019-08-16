package be.ledfan.geocoder.httpapi

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Routing
import org.kodein.di.Kodein

@KtorExperimentalLocationsAPI
class ReverseController(override val kodein: Kodein) : KodeinController(kodein) {

    private suspend fun reverse(call: ApplicationCall) {
        call.respond(mapOf("Hello" to "World"))
    }

    override fun Routing.registerRoutes() {
        get<Routes.Reverse> { reverse(this.call) }
    }

}