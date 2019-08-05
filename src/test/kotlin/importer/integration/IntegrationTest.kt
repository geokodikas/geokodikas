package importer.integration

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.importer.core.Importer
import be.ledfan.geocoder.importer.core.StatsCollector
import be.ledfan.geocoder.kodein
import importer.integration.containers.*
import kotlinx.coroutines.runBlocking
import org.kodein.di.direct
import org.kodein.di.generic.instance
import org.testcontainers.containers.PostgreSQLContainer

data class IntegrationConfig(val pbFurl: String, val pbfName: String, val pbfCheckSum: String)


class IntegrationTest(private val ic: IntegrationConfig) {

    init {
        val pbfFilePath = downloadAndCacheFile(ic.pbFurl, ic.pbfName, ic.pbfCheckSum) // TODO cache not working
        val importedImageName = "postgis_${ic.pbfName}_${ic.pbfCheckSum}"
        val fullImportedImageName = "full_import${ic.pbfName}_${ic.pbfCheckSum}"

        lateinit var postgresContainer: PostgreSQLContainer<KPostgreSQLContainer>
        // first check if container with this name exists
        if (imageExists(importedImageName)) {
            // start this image
            println("Target image $importedImageName already exists, starting that container")
            postgresContainer = postgresContainer(importedImageName)
        } else {
            // create and save image
            println("Target image $importedImageName does not exists, creating that image first")
            postgresContainer = postgresContainer()

            osm2psqlContainer(postgresContainer.currentContainerInfo.networkSettings.ipAddress,
                    postgresContainer.username, postgresContainer.password,
                    5432, postgresContainer.databaseName, pbfFilePath)

            // osm2psql has imported the data into the container, storing as new image
            commitContainer(postgresContainer.containerId, importedImageName)
            print("Start container from built image")
            postgresContainer = postgresContainer(importedImageName)
        }

        val config = kodein.direct.instance<Config>()
        config.database.jdbcUrl = postgresContainer.jdbcUrl
        config.database.username = postgresContainer.username
        config.database.password = postgresContainer.password

        kodein.direct.instance<StatsCollector>().suppressOutput = true
        val importer = kodein.direct.instance<Importer>()
        importer.setup(pbfFilePath)

        runBlocking {
            importer.executeStep("step0")
            importer.executeStep("step1")
            importer.executeStep("step2")
            importer.executeStep("step3")
        }

        // commiting container again
        println("Commiting as $fullImportedImageName")
        commitContainer(postgresContainer.containerId, fullImportedImageName)

//        ResultSet resultSet = performQuery (postgres, "SELECT 1");
//        int resultSetInt = resultSet . getInt (1);
//        assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
    }


}