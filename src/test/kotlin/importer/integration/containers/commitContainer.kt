package importer.integration.containers

import org.testcontainers.DockerClientFactory

fun commitContainer(currentContainerId: String, saveAsName: String) {
    val dockerClient = DockerClientFactory.instance().client()
    dockerClient.stopContainerCmd(currentContainerId)
            .withTimeout(360)
            .exec()
    dockerClient.commitCmd(currentContainerId).withRepository(saveAsName).exec()
}