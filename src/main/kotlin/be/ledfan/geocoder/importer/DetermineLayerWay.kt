package be.ledfan.geocoder.importer

import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.importer.core.Tags

class DetermineLayerWay : DetermineLayer() {

    // TODO traffic_signals maybe interesting
    // TODO traffic_calming

    fun determine(way: OsmWay, parsedTags: Tags): HashSet<Layer> {
        return determineHelper(way, parsedTags) { layers ->

            if (parsedTags.childOrNull("highway")?.hasValue("bus_stop") == true) {
                // ignore bus_stops
                assignLayer(layers, Layer.Superfluous)
                return@determineHelper // stop processing here
            }

            parsedTags.childOrNull("highway")?.let { highway ->
                if (highway.hasValue("motorway")
                        || highway.hasValue("trunk")
                        || highway.hasValue("primary")
                        || highway.hasValue("secondary")
                        || highway.hasValue("tertiary")
                        || highway.hasValue("unclassified")
                        || highway.hasValue("tunnel")
                        || highway.hasValue("road") // this should be tmp tags
                ) {
                    assignLayer(layers, Layer.Street)
                } else if (highway.hasValue("residential")
                        || highway.hasValue("living_street")
                        || highway.hasValue("pedestrian")
                        || highway.hasValue("footway")
                        || highway.hasValue("service")
                        || highway.hasValue("track")
                        || highway.hasValue("path")
                        || highway.hasValue("cycleway")
                        || highway.hasValue("steps")
                        || highway.hasValue("residentials")) {
                    // some houses/addresses are located on a highway of ^ such a type
                    // however they are only interesting if they have a name
                    if (parsedTags.hasChild("name")) {
                        assignLayer(layers, Layer.Street)
                    } else {
                        assignLayer(layers, Layer.Superfluous)
                    }
                } else if (
                        highway.hasValue("bridleway")
                        || highway.hasValue("services")
                        || highway.hasValue("rest_area")
                        || highway.hasValue("construction")
                        || highway.hasValue("dismantled")
                        || highway.hasValue("abandoned")
                        || highway.hasValue("disused")
                        || highway.hasValue("demolished")
                        || highway.hasValue("platform")
                        || highway.hasValue("ramp")
                        || highway.hasValue("raceway")
                        || highway.hasValue("corridor")
                        || highway.hasValue("proposed")
                        || highway.hasValue("planned")
                        || highway.hasValue("access_ramp")
                        || highway.hasValue("access")
                        || highway.hasValue("wall")
                        || highway.hasValue("bus_lane")
                        || highway.hasValue("bus_guideway")
                        || highway.hasValue("traffic_island")
                        || highway.hasValue("elevator")
                        || highway.hasValue("virtual")
                        || highway.hasValue("via_ferrata")
                        || highway.hasValue("emergency_access_point")
                        || highway.hasValue("slide")
                        || highway.hasValue("crossing")
                        || highway.hasValue("yes")
                        || highway.hasValue("disused")
                        || highway.hasValue("lanes") // TODO
                        || (parsedTags.childOrNull("access")?.singleValueOrNull() == "no")) {
                    assignLayer(layers, Layer.Superfluous)
                } else if (highway.hasValue("motorway_link")
                        || highway.hasValue("trunk_link")
                        || highway.hasValue("primary_link")
                        || highway.hasValue("secondary_link")
                        || highway.hasValue("tertiary_link")
                        || highway.hasValue("escape")
                ) {
                    assignLayer(layers, Layer.Link)
                } else if (highway.hasChild("virtual")
                        || highway.hasChild("disused")
                        || highway.hasChild("future")) {
                    assignLayer(layers, Layer.Superfluous)
                } else if (highway.hasValue("junction")) {
                    assignLayer(layers, Layer.Junction)
                } else {
                    logger.warn { "Found highway with unknown layer ${way.id}, ${parsedTags.toString(0)}" }
                    assignLayer(layers, Layer.Superfluous)
                }
            }
            parsedTags.childOrNull("place")?.let { place ->
                if (place.hasValue("square")
                        || place.hasValue("island")
                        || place.hasValue("plot")
                        || place.hasValue("farm")
                        || place.hasValue("isolated_dwelling")
                        || place.hasValue("hamlet")
                        || place.hasValue("village")
                        || place.hasValue("neighbourhood")
                        || place.hasValue("islet")) {
                    assignLayer(layers, Layer.Superfluous)
                }
            }

            parsedTags.childOrNull("area")?.let { area ->
                if (area.hasValue("yes")) {
                    assignLayer(layers, Layer.Superfluous)
                }
                if (area.hasChild("highway")) {
                    // ignore it
                    assignLayer(layers, Layer.Superfluous)
                }
                if (area.hasValue("courtyard")) {
                    // ignore it
                    assignLayer(layers, Layer.Superfluous)
                }
            }

            if (parsedTags.hasChild("name") && parsedTags.hasChild("landuse")) {
                if (parsedTags.hasChild("addr") && parsedTags.child("addr").hasChild("housenumber")) {
                    assignLayer(layers, Layer.Venue)
                } else if (parsedTags.child("landuse").singleValueOrNull() == "industrial"
                        && !parsedTags.hasChild("power")
                        && !parsedTags.hasChild("amenity")
                        && !parsedTags.hasChild("emergency")) {
                    assignLayer(layers, Layer.Street)
                } else {
                    assignLayer(layers, Layer.Superfluous)
                }
            } else {
                assignLayer(layers, Layer.Superfluous)
            }
            if (!layers.contains(Layer.Street) && parsedTags.hasChild("addr")) {
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

            parsedTags.childOrNull("service")?.let {
                // Currently assume it's a Street
                if (it.hasValue("alley")
                        || it.hasValue("driveway")
                        || it.hasValue("track")
                        || it.hasValue("parking_aisle")
                        || it.hasValue("parking_aside")) {
                    assignLayer(layers, Layer.Superfluous)
                }
            }
            parsedTags.childOrNull("route")?.let {
                // Currently assume it's a Street
                if (it.hasValue("foot")
                        || it.hasValue("walking")
                        || it.hasValue("running")
                        || it.hasValue("ferry")
                        || it.hasValue("bicycle")
                ) {
                    assignLayer(layers, Layer.Superfluous)
                }
            }

            if (parsedTags.childOrNull("type")?.hasValue("multipolygon") == true
                    || parsedTags.childOrNull("vehicle")?.hasValue("private") == true
                    || parsedTags.childOrNull("tunnel")?.hasValue("culvert") == true) {
                assignLayer(layers, Layer.Superfluous)
            }

            if (parsedTags.hasChild("junction")) {
                assignLayer(layers, Layer.Junction)
            }

            if (parsedTags.childOrNull("zone")?.hasChild("traffic") == true
                    || parsedTags.hasChild("maxspeed")) {
                assignLayer(layers, Layer.VirtualTrafficFlow)
            }
        }
    }

    fun importNodesForLayer(layer: Layer): Boolean {
        if (layer == Layer.Venue || layer == Layer.Address) {
            return false
        }

        return true
    }

    override val ignore = listOf("gate", "FIXME", "fixme", "Picture", "TMC", "abandoned", "access", "aerialway", "aeroway", "billboard", "boundary", "boundary_stone", "brewery", "chapel", "check", "cuisine", "culvert", "cycleway", "demolished", "description", "design", "destination", "disued", "disused", "door", "ele", "emergency", "entrance", "entry", "fenced", "foot", "ford", "gauge", "healthcare", "height", "historic", "history", "image", "landmark", "layer", "leisure", "loc_name", "location", "man_made", "mapillary", "military", "motorcar", "natural", "network", "office", "official_vicinal_ref", "opening_hours", "operator", "parking", "pipeline", "playground", "power", "priority", "public_transport", "rcn", "rcn_ref", "recycling", "ref", "relation", "rhn_ref", "river", "rwn", "rwn_ref", "seamark", "shelter", "shop", "source_rcn", "sport", "starting", "surface", "surveillance", "toll", "tourism", "tower", "tram", "waterway", "waypoint", "website", "amenity", "bicycle", "bollard", "lit", "mooring", "bench", "emergency_service", "placement", "right", "drive_through", "internet_access", "brand", "school", "guidepost", "note", "whitewater", "proposed", "lock", "denomination", "information", "contact", "animal", "maxheight", "author", "airpane", "craft", "carpenter", "cafe", "ladder", "endpoint", "razed", "continue", "stairs", "construction", "bicycle_parking", "todo", "water", "golf", "display", "ex_addr", "covered", "theme", "communication", "agricultural", "place_of_worship", "shootting", "tactile_paving", "container12", "car", "van", "backrest", "passenger_lines", "fuel", "barrier", "3dshapes", "designation", "material", "fireplace", "piste", "leaf_type", "leaf_cycle", "indoor", "landcover", "water_supply", "multipolygon", "wheelchair", "wikidata", "wikipedia", "embankment", "hti", "wetland", "bridge", "mtb", "street_cabinet", "kerb", "traffic_calming", "generator", "advertising", "crossing", "attraction", "water_slide", "tunnel", "allotments", "roof", "noname", "footway", "admin_level", "removed", "Meuleveldoop", "meadow", "was", "tracktype", "width", "ote", "destroyed", "crop", "verticalpassage", "capacity", "FID", "render", "agriculture", "old_name", "shed", "CABUTY", "garden", "wall", "fence_type", "grassland", "station", "cutting", "levels", "level", "camp_site", "tomb", "ramp", "length", "industrial", "conveying", "ruins", "bunker_type", "gun_turret", "smoothness", "tree_lined", "garden", "destroyed", "cemetery", "model_aerodrome", "nature", "lanes", "bar", "room", "building", "buildingpart", "house", "heritage")

    override val prune = listOf("railway")

}
