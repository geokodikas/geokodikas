package be.ledfan.geocoder.config

import java.io.File

data class Importer(
        var outputThreshold: Int = 10_000,
        var maxQueueSze: Int = 1_000_000,
        var numProcessors: Int = 16,
        var processorBlockSize: Int = 2_000,
        var countryId: Long = 52411L
)

data class ImportFromExport(
        var fileLocation: String = "",
        var fileMd5Sum: String = ""
)

data class Database(
        var username: String = "",
        var password: String = "",
        var jdbcUrl: String = "",
        var host: String = "",
        var dbName: String = ""
)

data class Runtime(
        var inputFileName: String = ""
)


data class Config(val importer: Importer,
                  val runtime: Runtime,
                  val database: Database,
                  val importFromExport: ImportFromExport,
                  var tmpDir: File = File(System.getProperty("user.dir") + "/tmp"))