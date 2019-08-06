package importer.integration

import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.steps.countTable
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BelgiumIntegrationTest : IntegrationTest(
        IntegrationConfig("http://download.geofabrik.de/europe/belgium-190801.osm.pbf",
                "belgium-190801.osm.pbf",
                "13728c4794819c8949c214dca25b1b36")) {

    @Test
    fun count() {
        assertEquals(570_695, countTable("osm_node"))
        assertEquals(2_085_688, countTable("osm_way"))
        assertEquals(1_850, countTable("osm_relation"))
        assertEquals(2_635_362, countTable("parent"))
        assertEquals(1_049_837, countTable("way_node"))
    }

    @Test
    fun main_relations() {

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
                53114L to "Antwerpen",
                58004L to "Vlaams-Brabant",
                416271L to "West-Vlaanderen",
                416271L to "West-Vlaanderen",
                53142L to "Limburg",
                78748L to "Brabant wallon",
                157559L to "Hainaut",
                1407192L to "Liège",
                1412581L to "Luxembourg",
                1311816L to "Namur")

        for ((id, name) in expectedCounties) {
            val county = relationMapper.getByPrimaryId(id)

            assertNotNull(county)
            assertEquals(name, county.name)
            assertEquals(Layer.County, county.layer)
        }
    }

}