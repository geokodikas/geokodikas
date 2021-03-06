package be.ledfan.geocoder.httpapi

import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.mapper.AddressIndexMapper
import be.ledfan.geocoder.db.mapper.OsmParentMapper
import be.ledfan.geocoder.db.mapper.WayNodeMapper
import com.beust.klaxon.JsonObject
import io.ktor.freemarker.FreeMarkerContent

class HTMLViewer(private val wayNodeMapper: WayNodeMapper, private val osmParentMapper: OsmParentMapper, private val addressIndexMapper: AddressIndexMapper) {

    private val htmlResponseBuilder = HTMLResponseBuilder()

    fun createHtml(geoJson: JsonObject, nodes: List<OsmNode>, ways: List<OsmWay>,
                   relations: List<OsmRelation>, addresses: List<AddressIndex>, order: List<Long>? = null): FreeMarkerContent {

        val allEntities = ways.toList() + nodes.toList() + relations.toList() + addresses.toList()
        val parents = osmParentMapper.getParents(allEntities)

        // Ways
        val nodesRelatedToWays = wayNodeMapper.getLinkedNodesByWay(ways)
        val addressesOnWays = addressIndexMapper.getByWays(ways)
        val tabs = htmlResponseBuilder.buildWay(ways, parents, nodesRelatedToWays, addressesOnWays)

        // nodes
        val waysRelatedToNodes = wayNodeMapper.getLinkedWaysByNodes(nodes.toList())
        tabs.putAll(htmlResponseBuilder.buildNode(nodes, parents, waysRelatedToNodes))

        // relations
        tabs.putAll(htmlResponseBuilder.buildRelation(relations, parents))

        // addresses
        tabs.putAll(htmlResponseBuilder.buildAddress(addresses, parents))

        return FreeMarkerContent("map_html.ftl",
                mapOf("geojson" to geoJson.toJsonString(true),
                        "tabs" to htmlResponseBuilder.buildTabs(tabs, order)), null)
    }

}