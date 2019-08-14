package be.ledfan.geocoder.importer.pipeline.containers

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.kodein
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import mu.KotlinLogging
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.security.MessageDigest
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors


fun md5sumOfFile(file: File): String {
    // inspired by https://stackoverflow.com/a/14922433

    val digest = MessageDigest.getInstance("MD5");
    file.forEachBlock(8196) { buffer, amountRead ->
        digest.update(buffer, 0, amountRead);
    }
    val bigInt = BigInteger(1, digest.digest())
    val output = bigInt.toString(16)

    // Fill to 32 chars
    return String.format("%32s", output).replace(' ', '0')
}

fun downloadAndCacheFile(url: String, destinationFileName: String, md5sum: String): String {
    val logger = KotlinLogging.logger { }
    val config: Config = kodein.direct.instance()
    val destinationFile = File(config.tmpDir, destinationFileName)

    if (destinationFile.exists() && md5sumOfFile(destinationFile) == md5sum) {
        logger.info("File already available with correct checksum, not downloading: $url")
        return destinationFile.absolutePath
    }

    logger.info("Downloading: $url to ${destinationFile.absolutePath}")

    val progressLock = Any()
    var previousProgress = 0

    val (request, response, result) = Fuel.download(url)
            .fileDestination { _, _ -> destinationFile }
            .progress { readBytes, totalBytes ->
                synchronized(progressLock) {
                    val progress = (readBytes.toFloat() / totalBytes.toFloat() * 100).toInt()
                    if (progress % 5 == 0 && progress != previousProgress) {
                        logger.info("Downloading $url: $progress%")
                        previousProgress = progress
                    }
                }
            }
            .response()

    when (result) {
        is Result.Success -> {
            val actualCheckSum = md5sumOfFile(destinationFile)
            if (actualCheckSum != md5sum) {
                throw Exception("Download of $url failed, expected : $md5sum, but download file has $actualCheckSum")
            }
            logger.info("File successfully downloaded, checksum: ${md5sumOfFile(destinationFile)}")
            return destinationFile.absolutePath
        }
        is Result.Failure -> {
            throw Exception("Error during download of $url", result.getException())
        }
    }

}

fun randomString(stringLength: Int = 8): String {
    val charPool = ('a'..'z') + ('0'..'9')
    return (1..stringLength)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("");
}

fun readListFromClassPath(resourceName: String): List<String> {
    val iS = object : Any() {}.javaClass.classLoader.getResourceAsStream(resourceName)
            ?: throw Exception("Resource '$resourceName' not found")

    return BufferedReader(InputStreamReader(iS, StandardCharsets.UTF_8)).lines().collect(Collectors.toList())
}
