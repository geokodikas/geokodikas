
import com.github.dockerjava.core.command.WaitContainerResultCallback
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging
import org.testcontainers.DockerClientFactory

fun commitContainer(currentContainerId: String, saveAsName: String): Boolean {
    val logger = KotlinLogging.logger {  }
    if (System.getenv("DO_NOT_COMMIT_CONTAINER") != null) {
        logger.warn("Not committing, because DO_NOT_COMMIT_CONTAINER env is set")
        return false
    }

    logger.info("Committing $currentContainerId as $saveAsName")

    val dockerClient = DockerClientFactory.instance().client()
    dockerClient.killContainerCmd(currentContainerId)
            .withSignal("SIGINT")
            .exec()

    runBlocking {
        withTimeoutOrNull(30000) {
            val r = dockerClient.waitContainerCmd(currentContainerId).exec(WaitContainerResultCallback())
            r.awaitCompletion()
        } ?: throw Exception("Stopping container timed out!")
    }

    dockerClient.commitCmd(currentContainerId).withRepository(saveAsName).exec()

    logger.info("Container committed")

    return true
}