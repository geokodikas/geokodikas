package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.addresses.HumanAddressBuilderService
import be.ledfan.geocoder.addresses.LangCode
import be.ledfan.geocoder.db.mapper.*
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
        // get address
        val addressIndex = addressIndexMapper.getByPrimaryId(route.id.toLong())
                ?: throw HttpApiNotFoundException("Address with id ${route.id} not found")
        addressIndexMapper.fetchRelations(addressIndex)

        val jsonResponseBuilder = JSONResponseBuilder()
        jsonResponseBuilder.addEntity(addressIndex)
        val geoJson = jsonResponseBuilder.toJson()

        val address = humanAddressBuilderService.build(LangCode.NL, addressIndex)
        addressIndex.dynamicProperties["Address"] = address

        if (getFormatting(call) == "html") {
            call.respond(htmlViewer.createHtml(geoJson = geoJson, addresses = listOf(addressIndex), ways = listOf(), nodes = listOf(), relations = listOf()))
        } else {
            call.respond(geoJson)
        }
    }


    override fun Routing.registerRoutes() {
        get<Routes.Address> { route ->
            withErrorHandling(call) {
                get(route, call)
            }
        }
    }

}


