package importer.integration

import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.steps.countTable
import org.intellij.lang.annotations.Language
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

    @Test
    fun relation_count_by_layer() {
        assertEquals(1, relationMapper.getByLayer(Layer.Country).size)
        assertEquals(3, relationMapper.getByLayer(Layer.MacroRegion).size)
        assertEquals(10, relationMapper.getByLayer(Layer.County).size)
//        assertEquals(578, relationMapper.getByLayer(Layer.LocalAdmin).size) // verified with overpass and wikipedia FIXME
        assertEquals(1227, relationMapper.getByLayer(Layer.Neighbourhood).size) // verified with overpass
    }

    @Test
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

//        assertEquals(expectedLocalAdmins, actualLocalAdmins) FIXME
    }

    // assert no records without any parent except for country

    @Test
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

    @Test
    fun `there are no nodes without a parent`() {
        // FIXME
        @Language("SQL")
        val stmt = """SELECT child.osm_id
            FROM osm_node AS child
            WHERE NOT EXISTS(SELECT *
                             FROM parent AS p1
                             WHERE p1.child_id = child.osm_id)
              -- If the node would have two parents, than it's fine it doesn't have any parent
              -- this is the case for nodes on the boundary of two LocalAdmins
              AND NOT (SELECT COUNT(*)
                       FROM osm_relation AS parent
                       where st_intersects(child.centroid, parent.geometry)
                         and parent.layer = 'LocalAdmin'::Layer) > 1
              -- Ignore points outside Belgium, they are used e.g. on roads crossing the boundary
              AND st_contains((SELECT geometry FROM osm_relation WHERE osm_id = 52411).geometry, child.centroid)
            """

        val ids = selectIds(stmt)

        assertEquals(arrayListOf<Long>(), ids)

    }

}