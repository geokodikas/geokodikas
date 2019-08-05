package importer.integration

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.importer.core.Importer
import be.ledfan.geocoder.importer.core.StatsCollector
import be.ledfan.geocoder.importer.steps.executeBatchQueries
import be.ledfan.geocoder.kodein
import importer.integration.containers.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import org.kodein.di.direct
import org.kodein.di.generic.instance
import org.testcontainers.containers.PostgreSQLContainer
import java.io.File

data class IntegrationConfig(val pbFurl: String, val pbfName: String, val pbfCheckSum: String)


class IntegrationTest(ic: IntegrationConfig) {

    private var postgresContainer: PostgreSQLContainer<KPostgreSQLContainer>

    private var logger = KotlinLogging.logger {}
    private val config = kodein.direct.instance<Config>()

    init {
        val pbfFilePath = downloadAndCacheFile(ic.pbFurl, ic.pbfName, ic.pbfCheckSum)
        val importedImageName = "postgis_${ic.pbfName}_${ic.pbfCheckSum}"
        val fullImportedImageName = "full_import${ic.pbfName}_${ic.pbfCheckSum}"

        // first check if container with this name exists
        var doImport = true
        when {
            imageExists(fullImportedImageName) -> {
                println("Target image $fullImportedImageName already exists, starting that container")
                postgresContainer = postgresContainer(fullImportedImageName)
                println("Container with image $fullImportedImageName started")
                doImport = false
            }
            imageExists(importedImageName) -> {
                // start this image
                println("Target image $importedImageName already exists, starting that container")
                postgresContainer = postgresContainer(importedImageName)
            }
            else -> {
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
        }

        config.database.jdbcUrl = postgresContainer.jdbcUrl
        config.database.username = postgresContainer.username
        config.database.password = postgresContainer.password
        config.importer.numProcessors = 8

        if (doImport) {
            kodein.direct.instance<StatsCollector>().suppressOutput = true
            val importer = kodein.direct.instance<Importer>()
            importer.setup(pbfFilePath)

            runBlocking {
                importer.executeStep("step0")
                importer.executeStep("step1")
                importer.executeStep("step2")
                importer.executeStep("step3")
                importer.finish()
            }

            // committing container again
            println("Committing as $fullImportedImageName")
            commitContainer(postgresContainer.containerId, fullImportedImageName)

        }
//        ResultSet resultSet = performQuery (postgres, "SELECT 1");
//        int resultSetInt = resultSet . getInt (1);
//        assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
    }

    fun dropUpstreamTables() {
        @Language("SQL")
        val sqlQueries = listOf(
                "DROP TABLE IF EXISTS osm_up_line",
                "DROP TABLE IF EXISTS osm_up_nodes",
                "DROP TABLE IF EXISTS osm_up_rels",
                "DROP TABLE IF EXISTS osm_up_ways",
                "DROP TABLE IF EXISTS osm_up_polygon",
                "DROP TABLE IF EXISTS osm_up_roads",
                "DROP TABLE IF EXISTS osm_up_point")

        executeBatchQueries(sqlQueries)
    }

    fun exportPostgisDb(fileName: String) {
        println("Going to export db")
        val res = postgresContainer.execInContainer("pg_dump", "-U", "test", "--verbose", "-Fc", "test", "-f", "/tmp/db.postgres")
        if (res.exitCode != 0) {
            throw Exception("Export postgis db failed")
        } else {
            println("Export postgis db succeeded")
        }

        postgresContainer.copyFileFromContainer("/tmp/db.postgres", File(config.tmpDir, fileName).absolutePath)

    }


}