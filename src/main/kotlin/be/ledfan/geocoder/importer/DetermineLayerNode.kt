package be.ledfan.geocoder.importer

import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.importer.core.Tags

class DetermineLayerNode : DetermineLayer() {

    // TODO traffic_signals may be interesting

    fun determine(node: OsmNode, parsedTags: Tags): HashSet<Layer> {
        return determineHelper(node, parsedTags) { layers ->

            parsedTags.childOrNull("junction")?.let {
                if (it.hasValue("traffic_signals")) {
                    assignLayer(layers, Layer.Superfluous)
                } else {
                    assignLayer(layers, Layer.Junction)
                }
            }

            parsedTags.childOrNull("junction_name")?.let {
                if (it.hasValue("traffic_signals")) {
                    assignLayer(layers, Layer.Superfluous)
                } else {
                    assignLayer(layers, Layer.Junction)
                }
            }

            parsedTags.childOrNull("highway")?.let { highway ->
                if (highway.hasAnyValue(listOf("motorway_junction", "junction", "mini_roundabout"))) {
                    assignLayer(layers, Layer.Junction)
                } else if (highway.hasAnyValue(listOf("turning_circle", "crossing", "turning_loop", "rest_area", "bus_stop", "incline", "incline1", "incline(1)", "incline_steep", "passing_place", "pasing_place", "street_lamp", "path", "wayside_shrine", "steps", "milestone", "traffic_sign", "services", "emergency_access_point", "ford", "elevator", "emergency_bay", "lift", "bicycle_crossing", "residential", "yes", "service", "ladder", "footway", "traffic_counter", "gate", "living_street", "platform", "via_ferrata", "srtr", "construction", "raceway", "no")) || highway.hasChild("virtual")) {
                    assignLayer(layers, Layer.Superfluous)
                } else if (highway.hasAnyValue(listOf("stop", "priority", "priority_right", "priority_on_right", "traffic_signals", "speed_camera", "traffic_mirror", "maxspeed", "street_sign", "mirror", "speed_display", "give_way"))) {
                    assignLayer(layers, Layer.VirtualTrafficFlow)
                } else if (highway.hasAnyValue(listOf("traffic_calming", "traffic_island", "hump"))) {
                    assignLayer(layers, Layer.PhysicalTrafficFlow)
                } else {
                    logger.warn { "Found highway with unknown layer ${node.id}, ${parsedTags.toString(0)}" }
                    assignLayer(layers, Layer.Superfluous)
                }
            }

            if (parsedTags.hasChild("crossing")) {
                // For now ignore every crossing (e.g. zebrapad)
                assignLayer(layers, Layer.Superfluous)
            }

            if (parsedTags.hasAnyChild(listOf("acces_sign", "access_sign", "street_sign", "enforcement", "traffic_sign", "traffic_signals", "maxweight", "city_limits", "traffic_sign", "noexit", "vehicle", "oneway", "maxspeed", "exit_to", "motorcard", "psv", "vehicle", "zone"))) {
                assignLayer(layers, Layer.VirtualTrafficFlow)
            }

            if (parsedTags.hasChild("traffic_calming") ||
                    parsedTags.childOrNull("change")?.hasChild("lanes") == true) {
                assignLayer(layers, Layer.PhysicalTrafficFlow)
            }

            if (parsedTags.hasChild("addr") && !parsedTags.hasChild("zone")) {
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

            if (parsedTags.hasAnyChild(listOf("turn", "incline", "barrier", "bridge", "service", "place", "hazard", "tracktype"))) {
                assignLayer(layers, Layer.Superfluous)
            }
        }
    }

    override val ignore = listOf("gate", "fixme", "FIXME", "Picture", "TMC", "abandoned", "access", "aerialway", "aeroway", "billboard", "boundary", "boundary_stone", "brewery", "chapel", "check", "cuisine", "culvert", "cycleway", "demolished", "description", "design", "destination", "disued", "disused", "door", "ele", "emergency", "entrance", "entry", "fenced", "foot", "ford", "gauge", "healthcare", "height", "historic", "history", "image", "landmark", "landuse", "layer", "leisure", "loc_name", "location", "man_made", "mapillary", "military", "motorcar", "natural", "network", "office", "official_vicinal_ref", "opening_hours", "operator", "parking", "pipeline", "playground", "power", "priority", "public_transport", "railway", "rcn", "rcn_ref", "recycling", "ref", "relation", "rhn_ref", "river", "rwn", "rwn_ref", "seamark", "shelter", "shop", "source_rcn", "sport", "starting", "surface", "surveillance", "toll", "tourism", "tower", "tram", "waterway", "waypoint", "website", "amenity", "bicycle", "bollard", "lit", "mooring", "bench", "emergency_service", "placement", "right", "drive_through", "internet_access", "brand", "school", "guidepost", "note", "whitewater", "proposed", "lock", "denomination", "information", "contact", "animal", "maxheight", "author", "airpane", "craft", "carpenter", "cafe", "ladder", "endpoint", "razed", "continue", "stairs", "construction", "bicycle_parking", "todo", "water", "golf", "display", "ex_addr", "covered", "theme", "communication", "agricultural", "place_of_worship", "shooting", "tactile_paving", "container12", "container6", "car", "van", "backrest", "passenger_lines", "fuel", "airplane", "kerb", "manhole", "indoor", "playground_equipment", "memorial", "advertising", "departures_board", "vending", "social_facility", "ruins", "landcover", "photo", "street_cabinet", "inscription", "religion", "leaf_type", "camp_site", "zoo", "area", "bus", "light_source", "pole", "transformer", "display_type", "beacon", "was", "brocante", "neon_light", "drinking_water", "generator", "continues", "xmas", "entr", "storage_area", "geological", "level", "health_facility", "disc_golf", "canoe", "fitness_station", "karst", "sidewalk", "club", "material", "mtb", "type", "(loc_name)", "mast", "attraction", "direction", "circulation", "tunnel", "code rue", "power_supply", "lanes", "roof", "industrial", "wetland", "factory", "wheelchair", "is_in", "building")

    override val prune = listOf("railway", "bus")

}
