package importer.integration.containers

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.WaitResponse
import com.github.dockerjava.core.command.WaitContainerResultCallback
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.testcontainers.DockerClientFactory

fun commitContainer(currentContainerId: String, saveAsName: String): Boolean {
    if (System.getenv("DO_NOT_COMMIT_CONTAINER") != null) {
        println("Not committing, because DO_NOT_COMMIT_CONTAINER env is set")
        return false
    }

    println("Committing $currentContainerId as $saveAsName")

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

    return true
}