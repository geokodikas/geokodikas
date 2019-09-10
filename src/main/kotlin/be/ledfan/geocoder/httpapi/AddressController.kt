package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.addresses.LangCode
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.mapper.*
import com.beust.klaxon.JsonObject
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Routing
import org.kodein.di.Kodein
import org.kodein.di.generic.instance

@KtorExperimentalLocationsAPI
class AddressController(override val kodein: Kodein) : KodeinController(kodein) {

    private val osmParentMapper: OsmParentMapper by instance()
    private val wayNodeMapper: WayNodeMapper by instance()
    private val addressIndexMapper: AddressIndexMapper by instance()
    private val htmlViewer = HTMLViewer(wayNodeMapper, osmParentMapper, addressIndexMapper)
    private val humanAddressBuilderService: HumanAddressBuilderService by instance()

    private suspend fun get(route: Routes.Address, call: ApplicationCall) {
        if (route.formatting == "json") {
            TODO("Not implemented")
        }

        // get address
        val addressIndex = addressIndexMapper.getByPrimaryId(route.id.toLong()) ?: TODO("Not implemented")
        addressIndexMapper.fetchRelations(addressIndex)

        val entity = addressIndex.entity ?: TODO("Not implemented")

        val jsonResponseBuilder = JSONResponseBuilder()
        jsonResponseBuilder.addEntity(entity)
        val geoJson = jsonResponseBuilder.toJson()

        val address = humanAddressBuilderService.build(LangCode.NL, addressIndex)
        entity.dynamicProperties["Address"] = address

        call.respond(htmlViewer.createHtmlForAddress(geoJson = geoJson, ways = listOf(entity)))
    }


    override fun Routing.registerRoutes() {
        get<Routes.Address> { route -> get(route, this.call) }
    }

}

