package importer.integration

import org.junit.jupiter.api.Test

class BasicIntegrationTest {


    @Test
    fun setup() {
//
//        val ic = IntegrationConfig("http://download.geofabrik.de/europe/belgium-190801.osm.pbf",
//                "belgium-190801.osm.pbf",
//                "13728c4794819c8949c214dca25b1b36")

        val ic = IntegrationConfig("http://download.openstreetmap.fr/extracts/europe/belgium.osm.pbf",
                "belgium.osm.pbf",
                "434e6864fab2ad77007e3e743a81a809")

//        val ic = IntegrationConfig("http://download.geofabrik.de/europe/liechtenstein-latest.osm.pbf",
//                "liechtenstein-latest.osm.pbf",
//                "342c794da60fabe59263664a7cc3377a")

        val x = IntegrationTest(ic)
//        x.dropUpstreamTables()
//        x.exportPostgisDb("full_import${ic.pbfName}_${ic.pbfCheckSum}")

    }


}