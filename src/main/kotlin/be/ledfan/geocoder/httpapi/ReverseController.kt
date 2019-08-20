package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.mapper.OsmWayMapper
import de.topobyte.osm4j.core.model.iface.OsmWay
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Routing
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.instance

@KtorExperimentalLocationsAPI
class ReverseController(override val kodein: Kodein) : KodeinController(kodein) {

    private suspend fun reverse(call: ApplicationCall) {

        // get some objects from DB
        val osmWayMapper = kodein.direct .instance<OsmWayMapper>()

        val entities = osmWayMapper.getByPrimaryIds(listOf(90582796, 90582719, 90582967))

        val responseBuilder = JSONResponseBuilder()
        entities.forEach { responseBuilder.addEntity(it.value) }

        call.respond(responseBuilder.buildAsCollection())
    }

    override fun Routing.registerRoutes() {
        get<Routes.Reverse> { reverse(this.call) }
    }

}