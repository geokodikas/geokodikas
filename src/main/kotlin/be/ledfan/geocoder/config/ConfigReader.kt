package be.ledfan.geocoder.config

import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonReader
import java.io.FileReader

object ConfigReader {


    fun getConfig(): Config {

        val importer = Importer()
        val database = Database()
        val config = Config(importer, Runtime(), database)

        JsonReader(FileReader("config.json")).use { reader ->

            reader.beginObject {

                while (reader.hasNext()) {
                    val readName = reader.nextName()
                    when (readName) {
                        "importer" -> readImporter(importer, reader.nextObject())
                        "database" -> readDatabase(database, reader.nextObject())
                    }
                }

            }

        }

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
        data.string("jdbc_url")?.let {
            database.jdbcUrl = it
        }
    }


}