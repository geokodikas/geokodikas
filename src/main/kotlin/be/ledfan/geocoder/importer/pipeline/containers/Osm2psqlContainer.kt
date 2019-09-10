package be.ledfan.geocoder.importer.pipeline.containers

import mu.KotlinLogging
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.OutputFrame
import java.util.concurrent.TimeUnit
import org.testcontainers.containers.output.WaitingConsumer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.utility.MountableFile
import java.io.File

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

fun osm2psqlContainer(dbHost: String, dbUser: String, dbPassword: String, dbPort: Int, dbName: String, pbfFilePath: String, numProc: String): KGenericContainer {
    val container = KGenericContainer("geokodikas/osm2pgsql:master")
            .withCreateContainerCmdModifier{ it.withName("osm2_pgsql_importer__${randomString()}") }
    container.withEnv("PG_HOST", dbHost)
    container.withEnv("PG_USER", dbUser)
    container.withEnv("PG_PORT", dbPort.toString())
    container.withEnv("PG_NAME", dbName)
    container.withEnv("PG_PASSWORD", dbPassword)
    container.withEnv("OSM2PSQL_NUM_PROC", numProc)
    container.withCopyFileToContainer(MountableFile.forHostPath(pbfFilePath), "/opt/geokodikas/input/input.pbf")

    val logger = KotlinLogging.logger {  }
    logger.info("Starting import, this will take some time")

    container.start()

    val logConsumer = Slf4jLogConsumer(KotlinLogging.logger {  })
    container.followOutput(logConsumer, OutputFrame.OutputType.STDERR)

    val consumer = WaitingConsumer()

    container.followOutput(consumer, OutputFrame.OutputType.STDERR)

    // first target
    consumer.waitUntil({ frame ->
        frame.utf8String.contains("Reading in file:")
    }, 300, TimeUnit.SECONDS)

    // second target
    consumer.waitUntil { frame ->
        frame.utf8String.contains("node cache: stored:")
    }

    // wait until fully finished
    consumer.waitUntilEnd()

    logger.info("Finished importing")

    // data should be imported now
    return container
}