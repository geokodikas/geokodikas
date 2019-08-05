package importer.integration.containers

import org.testcontainers.DockerClientFactory

fun imageExists(imageName: String): Boolean {
    val dockerClient = DockerClientFactory.instance().client();
    val images = dockerClient.listImagesCmd().exec()
    println(images)
    return images.any { image ->
        if (image.repoTags != null) {
            listOf<String>(*image.repoTags).contains("$imageName:latest")
        } else {
            false
        }
    }
}