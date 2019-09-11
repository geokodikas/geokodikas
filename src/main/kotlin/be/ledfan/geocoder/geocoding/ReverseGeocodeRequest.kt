package be.ledfan.geocoder.geocoding

import be.ledfan.geocoder.db.entity.OsmType
import be.ledfan.geocoder.httpapi.Routes
import be.ledfan.geocoder.importer.Layer
import io.ktor.application.ApplicationCall

data class ReverseGeocodeRequest(
        val formatting: String,
        val lat: Double,
        val lon: Double,
        val limitNumeric: Int,
        val limitRadius: Int,
        val limitLayers: List<Layer>,
        val hasLayerLimits: Boolean,
        val includeTags: List<String>?,
        val includeGeometry: Boolean) {

    val requiredTables: HashSet<OsmType> by lazy { getTablesForLayers(limitLayers) }

    companion object {

        val defaults = ReverseGeocodeRequest(
                "",
                0.0,
                0.0,
                5,
                200,
                listOf(Layer.Address, Layer.Venue, Layer.Street, Layer.Link),
                false,
                null,
                true)

        private fun parseList(input: String?): List<String>? {
            return input?.split(",")?.filter { it.trim() != "" }
        }

        fun createFromCall(route: Routes.Reverse, call: ApplicationCall): ReverseGeocodeRequest {
            val limitNumeric: Int = call.request.queryParameters["limitNumeric"]?.toInt() ?: defaults.limitNumeric
            val limitRadius: Int = call.request.queryParameters["limitRadius"]?.toInt() ?: defaults.limitRadius

            val providedLayers = parseList(call.request.queryParameters["limitLayers"])?.map { Layer.valueOf(it) }
            val (limitLayers, hasLayerLimits) =
                    if (providedLayers == null) {
                        Pair(defaults.limitLayers, defaults.hasLayerLimits)
                    } else {
                        Pair(providedLayers, true)
                    }
            val requiredTables = getTablesForLayers(limitLayers)
            val includeTags: List<String>? = parseList(call.request.queryParameters["includeTags"])
            val formatting = route.formatting
            val includeGeometry = if (formatting == "json") {
                call.request.queryParameters["includeGeometry"]?.toBoolean() ?: defaults.includeGeometry
            } else {
                true
            }

            return ReverseGeocodeRequest(
                    formatting,
                    route.lat,
                    route.lon,
                    limitNumeric,
                    limitRadius,
                    limitLayers,
                    hasLayerLimits,
                    includeTags,
                    includeGeometry
            )
        }
    }

}

