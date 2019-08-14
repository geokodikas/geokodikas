package be.ledfan.geocoder.importer.pipeline.implementation

import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.pipeline.AbstractValidator
import be.ledfan.geocoder.importer.pipeline.assertEquals
import be.ledfan.geocoder.importer.pipeline.assertNotNull
import be.ledfan.geocoder.importer.pipeline.containers.readListFromClassPath
import be.ledfan.geocoder.importer.steps.countTable
import org.intellij.lang.annotations.Language
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class BelgiumValidator : AbstractValidator() {

    @Validator
    fun `tables should have a fixed count`() {
        assertEquals(681_435L, countTable("osm_node"))
        assertEquals(2_128_877L, countTable("osm_way"))
        assertEquals(1_851L, countTable("osm_relation"))
        assertEquals(2_650_617L, countTable("parent"))
        assertEquals(1_102_112L, countTable("way_node"))
    }

    @Validator
    fun `should have specific relations`() {
        // Countries in this test
        val belgium = relationMapper.getByPrimaryId(52411L)
        assertNotNull(belgium)
        assertEquals("België / Belgique / Belgien", belgium.name)
        assertEquals(Layer.Country, belgium.layer)

        // MacroRegions in this test
        val flanders = relationMapper.getByPrimaryId(53134L)
        assertNotNull(flanders)
        assertEquals("Vlaanderen", flanders.name)
        assertEquals(Layer.MacroRegion, flanders.layer)

        val wallonia = relationMapper.getByPrimaryId(90348L)
        assertNotNull(wallonia)
        assertEquals("Wallonie", wallonia.name)
        assertEquals(Layer.MacroRegion, wallonia.layer)

        val brusselsRegion = relationMapper.getByPrimaryId(54094L)
        assertNotNull(brusselsRegion)
        assertEquals("Région de Bruxelles-Capitale - Brussels Hoofdstedelijk Gewest", brusselsRegion.name)
        assertEquals(Layer.MacroRegion, brusselsRegion.layer)

        // Counties in this test
        val expectedCounties = mapOf(
                53114L to Triple("Antwerpen", belgium, flanders),
                58004L to Triple("Vlaams-Brabant", belgium, flanders),
                416271L to Triple("West-Vlaanderen", belgium, flanders),
                416271L to Triple("West-Vlaanderen", belgium, flanders),
                53142L to Triple("Limburg", belgium, flanders),
                78748L to Triple("Brabant wallon", belgium, wallonia),
                157559L to Triple("Hainaut", belgium, wallonia),
                1407192L to Triple("Liège", belgium, wallonia),
                1412581L to Triple("Luxembourg", belgium, wallonia),
                1311816L to Triple("Namur", belgium, wallonia))

        for ((id, expectedData) in expectedCounties) {
            val (name, country, macroRegion) = expectedData
            val county = relationMapper.getByPrimaryId(id)

            assertNotNull(county)
            assertEquals(name, county.name)
            assertEquals(Layer.County, county.layer)
            val parents = relationMapper.getParents(county)
            assertEquals(2, parents.size)
            assertEquals(country.id, parents[Layer.Country]?.id)
            assertEquals(macroRegion.id, parents[Layer.MacroRegion]?.id)
        }
    }

    @Validator
    fun relation_count_by_layer() {
        assertEquals(1, relationMapper.getByLayer(Layer.Country).size)
        assertEquals(3, relationMapper.getByLayer(Layer.MacroRegion).size)
        assertEquals(10, relationMapper.getByLayer(Layer.County).size)
        assertEquals(581, relationMapper.getByLayer(Layer.LocalAdmin).size) // verified with overpass
        assertEquals(1227, relationMapper.getByLayer(Layer.Neighbourhood).size) // verified with overpass
    }

    @Validator
    fun check_local_admins() {
        /**
         * List of expected LocalAdmins can be generated using Overpass Turbo using:
         *
         * [out:csv(name)];
         * area["name"="België / Belgique / Belgien"]->.boundaryarea;
         * (
         *    relation(area.boundaryarea)[admin_level=8];
         * );
         * out;
         */

        val expectedLocalAdmins = readListFromClassPath("localadmins_belgium.txt").sorted()
        val actualLocalAdmins = relationMapper.getByLayer(Layer.LocalAdmin).map { it.value.name }.filterNotNull().sorted()
        assertEquals(expectedLocalAdmins, actualLocalAdmins)
    }

    @Validator
    fun check_neighbourhoods() {
        /**
         * List of expected LocalAdmins can be generated using Overpass Turbo using:
         *
         * [out:csv(name)];
         * area["name"="België / Belgique / Belgien"]->.boundaryarea;
         * (
         *    relation(area.boundaryarea)[admin_level=9];
         * );
         * out;
         */

        val expectedNeighbourhoods = readListFromClassPath("neighbourhoods_belgium.txt").sorted()
        val actualNeighbourhoods = relationMapper.getByLayer(Layer.Neighbourhood).map { it.value.name }.filterNotNull().sorted()

        assertEquals(expectedNeighbourhoods, actualNeighbourhoods)
    }

    @Validator
    fun `osm_node may only contain specific layers`() {
        @Language("SQL")
        val stmt = "SELECT DISTINCT layer, layer_order from osm_node"

        val layers = selectString(stmt, "layer").sorted()

        assertEquals(arrayListOf(
                "Address",
                "Junction",
                "PhysicalTrafficFlow",
                "Venue",
                "VirtualTrafficFlow"
        ), layers)
    }

    @Validator
    fun `osm_relation may only contain specific layers`() {
        @Language("SQL")
        val stmt = "SELECT DISTINCT layer, layer_order from osm_relation"

        val layers = selectString(stmt, "layer").sorted()

        assertEquals(arrayListOf(
                "Address", // TODO we probably don't want this in the osm_relation table
                "Venue", // TODO ^
                "Neighbourhood",
                "MacroRegion",
                "LocalAdmin",
                "County",
                "Country"
        ), layers)
    }

    @Validator
    fun `osm_way may only contain specific layers`() {
        @Language("SQL")
        val stmt = "SELECT DISTINCT layer, layer_order from osm_way"

        val layers = selectString(stmt, "layer").sorted()

        assertEquals(arrayListOf(
                "Address", // TODO we maybe don't want this in the osm_relation table
                "Junction",
                "Link",
                "Street",
                "Venue", // TODO ^
                "VirtualTrafficFlow"
        ), layers)
    }

    @Validator
    fun `way_node should have the same layers as in osm_way`() {
        @Language("SQL")
        val stmt = """
            SELECT osm_way.osm_id
                    FROM osm_way
                    JOIN way_node ON osm_way.osm_id = way_node.way_id
                    WHERE way_node.way_layer <> osm_way.layer
            or way_node.way_layer_order <> osm_way.layer_order
            """

        val ids = selectIds(stmt)
        assertEquals(listOf<Long>(), ids)
    }

    @Validator
    fun `way_node should have the same layers as in osm_node`() {
        @Language("SQL")
        val stmt = """
            SELECT osm_node.osm_id
                    FROM osm_node
                    JOIN way_node ON osm_node.osm_id = way_node.node_id
                    WHERE way_node.node_layer <> osm_node.layer
            or way_node.node_layer_order <> osm_node.layer_order
            """

        val ids = selectIds(stmt)
        assertEquals(listOf<Long>(), ids)
    }

    @Validator
    fun `nodes related to ways which are of type street has some specific layers`() {
        @Language("SQL")
        val stmt = """SELECT way_node.* FROM osm_way
            JOIN way_node ON osm_way.osm_id = way_node.way_id
            WHERE layer = 'Street'::Layer
                AND way_node.node_layer NOT IN ('Junction'::Layer, 'VirtualTrafficFlow'::Layer, 'PhysicalTrafficFlow'::Layer)
            """

        val ids = selectIds(stmt)
        assertEquals(listOf<Long>(), ids)
    }

    @Validator
    fun `streets should only contain node of some specific layers`() {
        @Language("SQL")
        val stmt = """SELECT way_node.*
            FROM osm_way
                 JOIN way_node ON osm_way.osm_id = way_node.way_id
            WHERE layer = 'Street'::Layer
              AND way_node.node_layer NOT IN ('Junction'::Layer, 'VirtualTrafficFlow'::Layer, 'PhysicalTrafficFlow'::Layer)"""

        val ids = selectIds(stmt)
        assertEquals(listOf<Long>(), ids)
    }

}