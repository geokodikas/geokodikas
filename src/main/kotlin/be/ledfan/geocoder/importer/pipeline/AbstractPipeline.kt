package be.ledfan.geocoder.importer.pipeline

import KPostgreSQLContainer
import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.ConnectionFactory
import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.mapper.ImportFromExportMetadataMapper
import be.ledfan.geocoder.importer.core.Importer
import be.ledfan.geocoder.importer.core.StatsCollector
import be.ledfan.geocoder.importer.pipeline.containers.*
import be.ledfan.geocoder.importer.steps.executeBatchQueries
import be.ledfan.geocoder.kodein
import commitContainer
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import org.kodein.di.direct
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.instance
import org.postgresql.util.PSQLException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT
import setupPostgresContainer
import java.io.File

data class IntegrationConfig(val pbFurl: String, val countryName: String, val pbfCheckSum: String)


abstract class AbstractPipeline(private val ic: IntegrationConfig) {

    private lateinit var postgresContainer: PostgreSQLContainer<KPostgreSQLContainer>

    private var logger = KotlinLogging.logger {}
    private val config = kodein.direct.instance<Config>()

    suspend fun import() {

        val pbfFilePath = downloadAndCacheFile(ic.pbFurl, "${ic.countryName}_${ic.pbfCheckSum}.pbf", ic.pbfCheckSum)
        val importedImageName = "postgis_${ic.countryName}_${ic.pbfCheckSum}"
        val fullImportedImageName = "full_import${ic.countryName}_${ic.pbfCheckSum}"

        // first check if container with this name exists
        var doImport = true
        when {
            imageExists(fullImportedImageName) -> {
                logger.info("Target image $fullImportedImageName already exists, starting that container")
                postgresContainer = setupPostgresContainer(fullImportedImageName)
                logger.info("Container with image $fullImportedImageName started")
                doImport = false
            }
            imageExists(importedImageName) -> {
                // start this image
                logger.info("Intermediate image $importedImageName already exists, starting that container")
                postgresContainer = setupPostgresContainer(importedImageName)
            }
            else -> {
                // create and save image
                logger.info("Target image $importedImageName does not exists, creating that image first")
                postgresContainer = setupPostgresContainer()

                osm2psqlContainer(postgresContainer.currentContainerInfo.networkSettings.ipAddress,
                        postgresContainer.username, postgresContainer.password,
                        POSTGRESQL_PORT,
                        postgresContainer.databaseName, pbfFilePath, config.importer.numProcessors.toString())

                // osm2psql has imported the data into the container, storing as new image
                val committed = commitContainer(postgresContainer.containerId, importedImageName)
                if (committed) {
                    logger.info("Start container from built image")
                    postgresContainer = setupPostgresContainer(importedImageName)
                }
            }
        }

        reConnectAllConnections()


        if (doImport) {
            kodein.direct.instance<StatsCollector>().suppressOutput = true
            val importer = kodein.direct.instance<Importer>()
            importer.setup(pbfFilePath)

            runBlocking {
                importer.executeStep("step0")
                importer.executeStep("step1")
                importer.executeStep("step2")
                importer.executeStep("step3")
                importer.executeStep("step4")
                importer.finish()
            }

            closeAllConnections() // connections must be closed for safe shutdown of postgres

            // committing container again
            val commited = commitContainer(postgresContainer.containerId, fullImportedImageName)
            if (commited) {
                logger.info("Start container from built image")
                postgresContainer = setupPostgresContainer(fullImportedImageName)
            }
            reConnectAllConnections()
        }
    }

    abstract fun validate(): Boolean

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

    fun export(): Pair<String, String> {
        val fileName = "full_import_${ic.countryName}_${ic.pbfCheckSum}__${randomString()}"

        logger.info("Going to export db into $fileName")
        val res = postgresContainer.execInContainer("pg_dump", "-U", "geokodikas", "--verbose", "-Fc", "geokodikas", "-f", "/tmp/db.postgres")
        if (res.exitCode != 0) {
            throw Exception("Export postgis db failed")
        } else {
            logger.info("Export db succeeded")
        }

        val finalFile = File(config.tmpDir, fileName)
        val finalPath = finalFile.absolutePath
        postgresContainer.copyFileFromContainer("/tmp/db.postgres", finalPath)
        logger.info("Db export available at $finalPath")
        return Pair(fileName, md5sumOfFile(finalFile))
    }

    private fun closeAllConnections() {
        // first closing all connection to the db from this process
        val connections: List<ConnectionWrapper> by kodein.allInstances()
        connections.forEach { it.close() }
        // then terminate connections from other processes
        val con = ConnectionFactory.createConnection(config)
        try {
            con.prepareStatement("select pg_terminate_backend(pid) from pg_stat_activity").execute()
        } catch (e: PSQLException) {
            // we probably get an exception because our connection got closed
        } finally {
            con.close()
        }
        // now all connections should be closed
    }

    private fun reConnectAllConnections() {
        config.database.username = postgresContainer.username
        config.database.password = postgresContainer.password
        config.database.host = postgresContainer.containerIpAddress
        config.database.dbName = postgresContainer.databaseName
        config.database.port = postgresContainer.getMappedPort(POSTGRESQL_PORT)

        val connections: List<ConnectionWrapper> by kodein.allInstances()
        connections.forEach { it.reConnect() }
    }

}