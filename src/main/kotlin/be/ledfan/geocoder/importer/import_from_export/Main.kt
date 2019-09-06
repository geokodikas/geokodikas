package be.ledfan.geocoder.importer.import_from_export

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.importer.pipeline.containers.downloadAndCacheFile
import be.ledfan.geocoder.kodein
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import org.kodein.di.direct
import org.kodein.di.generic.instance

fun importFromExport() {

    val config = kodein.direct.instance<Config>()
    val logger = KotlinLogging.logger {}

    if (config.importFromExport.fileLocation.isEmpty()
            || config.importFromExport.fileMd5Sum.isEmpty()) {
        throw Exception("Provide the 'import_from_export.file_location' and  'import_from_export.file_md5sum' parameters in config.json please")
    }

    // check if we can connect to the configured database
    val connection = kodein.direct.instance<ConnectionWrapper>()

    if (!dbHasNoImport(connection)) {
        throw Exception("Database already contains a part of the schema (checked for type 'Layer' and table 'osm_way')")
    }

    // download file
    val absolutePath = downloadAndCacheFile(config.importFromExport.fileLocation, "import_from_export_${config.importFromExport.fileMd5Sum}", config.importFromExport.fileMd5Sum)

    val processBuilder = ProcessBuilder("pg_restore", "-v",
            "-U", config.database.username,
            "-d", config.database.dbName,
            "-h", config.database.host,
            "-j8", absolutePath)

    val env = processBuilder.environment()
    env["PGPASSWORD"] = config.database.password

    logger.info { "Starting import process" }
    val process = processBuilder.start()

    process.inputStream.reader(Charsets.UTF_8).use {
        logger.info { "STDOUT: ${it.readText()}" }
    }
    process.errorStream.reader(Charsets.UTF_8).use {
        logger.info { "STDERR: ${it.readText()}" }
        process.waitFor()

        if (process.exitValue() == 0) {
            logger.info ("Import was a success")
        } else {
            throw Exception("Error during import")
        }

    }
}

/**
 * Perform some checks to ensure the database in which we are importing is empty.
 */
fun dbHasNoImport(connection: ConnectionWrapper): Boolean {
    return !(sqlExists(connection, "SELECT 1 as one FROM pg_type WHERE typname = 'layer' and typinput = 'enum_in'::regproc")
            || sqlExists(connection, "SELECT 1 as one FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'osm_way'"))
}

/**
 * Executes a simple query and returns whether it returned the value `1` for the column one.
 */

fun sqlExists(connection: ConnectionWrapper, @Language("SQL") query: String): Boolean {
    val stmt = connection.prepareStatement(query)
    val result = stmt.executeQuery()

    return result.next() && result.getInt("one") == 1
}
