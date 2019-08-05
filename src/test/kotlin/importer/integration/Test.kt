package importer.integration

import org.junit.jupiter.api.Test

class BasicIntegrationTest {


    @Test
    fun basic_test() {

        val ic = IntegrationConfig("http://download.geofabrik.de/europe/belgium-190731.osm.pbf",
                "belgium-190731.osm.pbf",
                "b73f9944980e5796a3eeab810014e328")

//        val ic = IntegrationConfig("http://download.geofabrik.de/europe/liechtenstein-latest.osm.pbf",
//                "liechtenstein-latest.osm.pbf",
//                "d1b56be3964600e8ac1e57cac1b11e25")

        val x = IntegrationTest(ic)

    }


}