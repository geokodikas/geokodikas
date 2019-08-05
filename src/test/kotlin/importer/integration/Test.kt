package importer.integration

import org.junit.jupiter.api.Test

class BasicIntegrationTest {


    @Test
    fun basic_test() {

        val ic = IntegrationConfig("http://download.geofabrik.de/europe/belgium-190801.osm.pbf",
                "belgium-190801.osm.pbf",
                "13728c4794819c8949c214dca25b1b36")

//        val ic = IntegrationConfig("http://download.geofabrik.de/europe/liechtenstein-latest.osm.pbf",
//                "liechtenstein-latest.osm.pbf",
//                "d1b56be3964600e8ac1e57cac1b11e25")

        val x = IntegrationTest(ic)
        x.dropUpstreamTables()
        x.exportPostgisDb("full_import${ic.pbfName}_${ic.pbfCheckSum}")

    }


}