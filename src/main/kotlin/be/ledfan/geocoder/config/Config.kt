package be.ledfan.geocoder.config

data class Importer(
        var outputThreshold: Int = 10_000,
        var maxQueueSze: Int = 1_000_000,
        var numProcessors: Int = 16,
        var processorBlockSize: Int = 2_000,
        var countryId: Long = 52411L
)

data class Database(
        var username: String = "",
        var password: String = "",
        var jdbcUrl: String = ""
)

data class Runtime(
        var inputFileName: String = ""
)


data class Config(val importer: Importer, val runtime: Runtime, val database: Database)