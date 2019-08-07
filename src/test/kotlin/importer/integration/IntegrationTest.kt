package importer.integration

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.ConnectionFactory
import be.ledfan.geocoder.db.mapper.OsmRelationMapper
import be.ledfan.geocoder.importer.core.Importer
import be.ledfan.geocoder.importer.core.StatsCollector
import be.ledfan.geocoder.importer.steps.executeBatchQueries
import be.ledfan.geocoder.kodein
import importer.integration.containers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import org.kodein.di.direct
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.instance
import org.postgresql.util.PSQLException
import org.testcontainers.containers.PostgreSQLContainer
import java.io.File
import java.lang.Thread.sleep
import java.sql.Connection

data class IntegrationConfig(val pbFurl: String, val pbfName: String, val pbfCheckSum: String)


open class IntegrationTest(ic: IntegrationConfig) {

    private var postgresContainer: PostgreSQLContainer<KPostgreSQLContainer>

    private var logger = KotlinLogging.logger {}
    private val config = kodein.direct.instance<Config>()
    protected var relationMapper: OsmRelationMapper
    protected var con: Connection

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
                println("Intermediate image $importedImageName already exists, starting that container")
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

        if (ConnectionFactory.createdConnections != 0) {
            println("Already created connections before changing config")
        }

        relationMapper = kodein.direct.instance() // bind now after conncetion can be made
        con = kodein.direct.instance()

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

            closeAllConnections() // connections must be closed for safe shutdown of postgres

            // committing container again
            println("Committing as $fullImportedImageName")
            commitContainer(postgresContainer.containerId, fullImportedImageName)
        }
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

    private fun closeAllConnections() {
        // first closing all connection to the db from this process
        val connections: List<Connection> by kodein.allInstances()
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

    fun selectIds(query: String): ArrayList<Long> {
        val stmt = con.prepareStatement(query)
        val result = stmt.executeQuery()

        val ids = arrayListOf<Long>()

        while (result.next()) {
            ids.add(result.getLong("osm_id"))
        }

        stmt.close()
        result.close()

        return ids
    }


}