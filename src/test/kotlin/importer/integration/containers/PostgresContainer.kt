package importer.integration.containers

import importer.integration.randomString
import mu.KotlinLogging
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.output.WaitingConsumer
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS
import java.util.concurrent.TimeUnit

class KPostgreSQLContainer(dockerImageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(dockerImageName) {

    init {
        this.waitStrategy = org.testcontainers.containers.wait.LogMessageWaitStrategy()
                .withRegEx(".*database system is ready to accept connections.*\\s")
                .withTimes(1) // Very important that this is 1 instead of 2 for containers which already
                // contain a postgresql data directory, either as volume or with a committed container
                .withStartupTimeout(Duration.of(60, SECONDS))
    }

    override fun configure() {
        super.configure()

        setCommand("postgres", "-c", "config_file=/etc/postgresql/postgresql.conf")
    }
}

fun postgresContainer(): PostgreSQLContainer<KPostgreSQLContainer> = postgresContainer("ledfan/postgis_hstore:latest")

fun postgresContainer(existingImage: String): KPostgreSQLContainer {
    val postgresContainer = KPostgreSQLContainer(existingImage)
            .withCreateContainerCmdModifier{ it.withName("postgis__${randomString()}") }
    postgresContainer.start()
    val logConsumer = Slf4jLogConsumer(KotlinLogging.logger { })
    postgresContainer.followOutput(logConsumer, OutputFrame.OutputType.STDERR)

    val waitingConsumer = WaitingConsumer()
    postgresContainer.followOutput(waitingConsumer, OutputFrame.OutputType.STDERR)
    waitingConsumer.waitUntil({ frame ->
        frame.utf8String.contains("database system is ready to accept connections")
    }, 60, TimeUnit.SECONDS, 1)

    return postgresContainer
}
