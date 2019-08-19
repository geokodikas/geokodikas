package be.ledfan.geocoder.httpapi

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Routing
import org.kodein.di.Kodein

@KtorExperimentalLocationsAPI
class OverviewController(override val kodein: Kodein) : KodeinController(kodein) {

    private suspend fun getRoot(call: ApplicationCall) {
        val urls = Routes.definedRoutes.map { "http://localhost:8080" + it.href } // TODO

        call.respond(mapOf("urls" to urls))
    }

    override fun Routing.registerRoutes() {
        get<Routes.Root> { getRoot(this.call) }
    }

}