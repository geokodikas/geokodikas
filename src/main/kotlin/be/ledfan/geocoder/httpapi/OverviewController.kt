package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.config.Config
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.locations.locations
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.routing.Routing
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.instance

@KtorExperimentalLocationsAPI
class OverviewController(override val kodein: Kodein) : KodeinController(kodein) {

    private val config = kodein.direct.instance<Config>()

    private suspend fun getRoot(call: ApplicationCall) {
        val urls = ArrayList<String>()
        for (route in Routes.exampleRoutes) {
            urls.add(config.http.publicUrl + application.locations.href(route, listOf(Pair("formatting", "html"))))
            urls.add(config.http.publicUrl + application.locations.href(route, listOf(Pair("formatting", "json"))))
        }
        call.respond(mapOf("urls" to urls))
    }

    override fun Routing.registerRoutes() {
        get<Routes.Root> { getRoot(this.call) }
    }

}