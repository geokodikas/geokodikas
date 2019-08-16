package be.ledfan.geocoder.importer.import_from_export

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.importer.pipeline.containers.md5sumOfFile
import be.ledfan.geocoder.kodein
import ch.qos.logback.classic.util.ContextInitializer
import org.intellij.lang.annotations.Language
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.io.File
import kotlin.system.exitProcess


fun main(args: Array<String>) {

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.pipeline.xml");

    val config = kodein.direct.instance<Config>()

    if (config.importFromExport.fileLocation.isEmpty()
            || config.importFromExport.fileMd5Sum.isEmpty()) {
        println("Provide the 'import_from_export.file_location' and  'import_from_export.file_md5sum' parameters in config.json please")
        exitProcess(1)
    }

    // check if we can connect to the configured database
    val connection = kodein.direct.instance<ConnectionWrapper>()
    val hasConnection = try {
        sqlExists(connection, "SELECT 1 as one")
    } catch (e: Exception) {
        println(e.printStackTrace())
        false
    }

    if (!hasConnection) {
        println("Could not establish a working connection.")
        exitProcess(1)
    }

    if (!dbHasNoImport(connection)) {
        println("Database already contains a part of the schema (checked for type 'Layer' and table 'osm_way')")
        exitProcess(1)
    }

    // validate checksum
    val actual = md5sumOfFile(File(config.importFromExport.fileLocation))
    if (actual != config.importFromExport.fileMd5Sum) {
        println("Checksum of file to import is wrong, expected ${config.importFromExport.fileMd5Sum}, actual: $actual")
        exitProcess(1)
    }

    val processBuilder = ProcessBuilder("pg_restore", "-v",
            "-U", config.database.username,
            "-d", config.database.dbName,
            "-h", config.database.host,
            "-j8", config.importFromExport.fileLocation)

    val env = processBuilder.environment()
    env["PGPASSWORD"] = config.database.password

    println("Starting import process")
    val process = processBuilder.start()

    process.inputStream.reader(Charsets.UTF_8).use {
        println("STDOUT: ${it.readText()}")
    }
    process.errorStream.reader(Charsets.UTF_8).use {
        println("STDERR: ${it.readText()}")
    }
    process.waitFor()

    if (process.exitValue() == 0) {
        println("Successfully imported")
    } else {
        println("Error during imported")
    }

    connection.close()
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
