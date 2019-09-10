package be.ledfan.geocoder.importer.pipeline.containers

import org.testcontainers.DockerClientFactory

fun imageExists(imageName: String): Boolean {
    val dockerClient = DockerClientFactory.instance().client();
    val images = dockerClient.listImagesCmd().exec()
    val imageNameLower = imageName.toLowerCase()
    return images.any { image ->
        if (image.repoTags != null) {
            listOf<String>(*image.repoTags).contains("$imageNameLower:latest")
        } else {
            false
        }
    }
}