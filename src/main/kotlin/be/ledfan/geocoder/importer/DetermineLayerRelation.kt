package be.ledfan.geocoder.importer

import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.importer.core.Tags

class DetermineLayerRelation : DetermineLayer() {

    // TODO traffic_signals maybe interesting
    // TODO traffic_calming

    private fun determineLayerForBoundary(parsedTags: Tags, layers: HashSet<Layer>) {
        if (parsedTags.childOrNull("boundary")?.hasAnyValue(listOf("postal_code", "political", "administrative_fraction")) == true) {
            // completely ignore "administrative_fraction", this is e.g. "Wallonië (Franse Gemeenschap)" where
            // we prefer Gewesten
            layers.clear()
            assignLayer(layers, Layer.Superfluous)
            return
        }
        parsedTags.childOrNull("admin_level")?.let { adminLevel ->
            when {
                adminLevel.hasValue("1") -> assignLayer(layers, Layer.Superfluous) // only europe?
                adminLevel.hasValue("2") -> assignLayer(layers, Layer.Country)
                adminLevel.hasValue("3") -> assignLayer(layers, Layer.Superfluous)
                adminLevel.hasValue("4") -> assignLayer(layers, Layer.MacroRegion) // Vlaams gewest etc
                adminLevel.hasValue("5") -> assignLayer(layers, Layer.Superfluous)
                adminLevel.hasValue("6") -> assignLayer(layers, Layer.County)
                adminLevel.hasValue("7") -> assignLayer(layers, Layer.Superfluous) // administrative arrondissements  -> not useful
                adminLevel.hasValue("8") -> assignLayer(layers, Layer.LocalAdmin)
                adminLevel.hasValue("9") -> assignLayer(layers, Layer.Neighbourhood)
                adminLevel.hasValue("10") -> assignLayer(layers, Layer.Superfluous) // N/A
            }
        }
    }

    fun determine(relation: OsmRelation, parsedTags: Tags): HashSet<Layer> {
        return determineHelper(relation, parsedTags) { layers ->

            parsedTags.childOrNull("type")?.let { type ->
                if (type.hasAnyValue(listOf("dual_carriageway", "bridge", "street", "route", "associatedStreet", "collection", "set", "site", "turnlanes:turns", "turnlanes:length", "turnlanes:lengths", "route_master", "circuit", "public_transport", "TMC", "organization", "destination_sign", "stop", "provides_feature", "region", "level", "building", "restriction", "transit", "military", "through_route", "waterway"))) {
                    assignLayer(layers, Layer.Superfluous)
                }
                if (type.hasValue("boundary")) {
                    if (parsedTags.hasChild("admin_level") || parsedTags.hasValue("border_type")) {
                        determineLayerForBoundary(parsedTags, layers)
                    } else {
                        assignLayer(layers, Layer.Superfluous)
                    }
                }
                if (type.hasValue("land_area")) {
                    if (parsedTags.hasChild("admin_level")) {
                        determineLayerForBoundary(parsedTags, layers)
                    }
                }
                if (type.hasAnyValue(listOf("access", "traffic_signals", "traffic_signals_group", "give_way", "traffic_mirror", "enforcement"))) {
                    assignLayer(layers, Layer.VirtualTrafficFlow)
                }
                if (type.hasValue("junction")) {
                    assignLayer(layers, Layer.Junction)
                }
                if (type.hasValue("defaults")) {
                    assignLayer(layers, Layer.Superfluous)
                }
            }

            if (parsedTags.childOrNull("highway")?.hasAnyValue(listOf("pedestrian", "tertiary", "unclassified", "path", "track", "service", "footway")) == true) {
                assignLayer(layers, Layer.Superfluous)
            }

            if (parsedTags.hasChild("boundary") && parsedTags.hasChild("admin_level")) {
                determineLayerForBoundary(parsedTags, layers)
            }

            if (parsedTags.childOrNull("boundary")?.hasValue("political") == true) {
                assignLayer(layers, Layer.Superfluous)
            }

            if (parsedTags.hasAnyValue(listOf("restriction", "zone", "access", "enforcement", "maxspeed"))) {
                assignLayer(layers, Layer.VirtualTrafficFlow) // FIXME
            }

            if (parsedTags.childOrNull("network")?.hasAnyValue(listOf("rcn", "iwn", "IBXL", "road", "rwn", "Villo!", "public_transport", "hiking")) == true) {
                assignLayer(layers, Layer.Superfluous)
            }

            if (layers.none { it != Layer.Superfluous } && parsedTags.hasChild("addr")) {
                if (parsedTags.hasChild("name")) {
                    assignLayer(layers, Layer.Venue)
                } else {
                    assignLayer(layers, Layer.Address)
                }
            }

            if (parsedTags.hasChild("name")) {
                // Fall back to superfluous when the way has name but no other useful tags
                assignLayer(layers, Layer.Superfluous)
            }

            if (parsedTags.hasAnyChild(listOf("area", "ref", "foot", "motor_vehicle", "bicycle", "layer", "height", "public_transport", "description", "role"))) {
                assignLayer(layers, Layer.Superfluous)
            }
        }
    }


    override val ignore = listOf("fixme", "FIXME", "building", "natural", "landuse", "site", "parking", "waterway", "leisure", "barrier", "man_made", "heritage", "sport", "power", "route", "TMC", "buildingpart", "tourism", "amenity", "landcover", "leaf_type", "surface", "indoor", "service", "historic", "designation", "place", "aeroway", "military")
}