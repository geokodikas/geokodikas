package be.ledfan.geocoder.config

import ch.qos.logback.core.util.OptionHelper.getEnv
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonReader
import java.io.File
import java.io.FileReader

object ConfigReader {


    fun getConfig(): Config {

        val importer = Importer()
        val database = Database()
        val importFromExport = ImportFromExport()
        val http = Http()
        val config = Config(importer, Runtime(), database, importFromExport, http)

        JsonReader(FileReader("config.json")).use { reader ->

            reader.beginObject {

                while (reader.hasNext()) {
                    val readName = reader.nextName()
                    when (readName) {
                        "importer" -> readImporter(importer, reader.nextObject())
                        "database" -> readDatabase(database, reader.nextObject())
                        "tmp_dir" -> config.tmpDir = File(reader.nextString())
                        "import_from_export" -> readImportFromExport(importFromExport, reader.nextObject())
                        "http" -> readHttp(http, reader.nextObject())
                    }
                }

            }

        }

        if (!config.tmpDir.exists()) {
            config.tmpDir.mkdirs()
        }

        loadMissingSettingsFromEnv(config)

        return config
    }

    private fun readImporter(importer: Importer, data: JsonObject) {
        data.int("output_threshold")?.let {
            importer.outputThreshold = it
        }

        data.int("max_queue_size")?.let {
            importer.maxQueueSze = it
        }

        data.int("num_processors")?.let {
            importer.numProcessors = it
        }

        data.int("processor_block_size")?.let {
            importer.processorBlockSize = it
        }
    }

    private fun readDatabase(database: Database, data: JsonObject) {
        data.string("username")?.let {
            database.username = it
        }
        data.string("password")?.let {
            database.password = it
        }
        data.string("db_name")?.let {
            database.dbName = it
        }
        data.string("host")?.let {
            database.host = it
        }
    }

    private fun readImportFromExport(importFromExport: ImportFromExport, data: JsonObject) {
        data.string("file_location")?.let {
            importFromExport.fileLocation = it
        }

        data.string("file_md5sum")?.let {
            importFromExport.fileMd5Sum = it
        }

        data.boolean("try_import_on_http")?.let {
            importFromExport.tryImportOnHttp = it
        }
    }

    private fun readHttp(http: Http, data: JsonObject) {
        data.string("public_url")?.let {
            http.publicUrl = it
        }
    }

    private fun loadMissingSettingsFromEnv(config: Config) {
        if (config.database.username == "") {
            getEnv("GEOKODIKAS_DB_USERNAME")?.let {
                config.database.username = it
            }
        }
        if (config.database.password == "") {
            getEnv("GEOKODIKAS_DB_PASSWORD")?.let {
                config.database.password = it
            }
        }
        if (config.database.host == "") {
            getEnv("GEOKODIKAS_DB_HOST")?.let {
                config.database.host = it
            }
        }
        if (config.database.port == 5432) {
            getEnv("GEOKODIKAS_DB_PORT")?.let {
                config.database.port = it.toInt()
            }
        }
        if (config.database.dbName == "") {
            getEnv("GEOKODIKAS_DB_NAME")?.let {
                config.database.dbName = it
            }
        }
    }

    private fun loadMissingSettingsFromEnv(config: Config) {
        if (config.database.username == "") {
            getEnv("GEOKODIKAS_DB_USERNAME")?.let {
                config.database.username = it
            }
        }
        if (config.database.password == "") {
            getEnv("GEOKODIKAS_DB_PASSWORD")?.let {
                config.database.password = it
            }
        }
        if (config.database.host == "") {
            getEnv("GEOKODIKAS_DB_HOST")?.let {
                config.database.host = it
            }
        }
        if (config.database.port == 5432) {
            getEnv("GEOKODIKAS_DB_PORT")?.let {
                config.database.port = it.toInt()
            }
        }
        if (config.database.dbName == "") {
            getEnv("GEOKODIKAS_DB_NAME")?.let {
                config.database.dbName = it
            }
        }

    }

}