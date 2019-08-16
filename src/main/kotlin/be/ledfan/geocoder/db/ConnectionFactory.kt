package be.ledfan.geocoder.db

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.config.ConfigReader
import mu.KotlinLogging
import org.postgresql.util.PSQLException
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import kotlin.system.exitProcess


object ConnectionFactory {

    var createdConnections: Int = 0

    fun createConnection(config: Config): Connection {
        val properties = Properties()

        with(properties) {
            put("user", config.database.username)
            put("password", config.database.password)
        }


        val jdbcUrl = "jdbc:postgresql://${config.database.host}/${config.database.dbName}"

        // Open a connection to the database
        try {
            KotlinLogging.logger {}.trace { "Trying to get connections currently at $createdConnections connections" }
            val r = DriverManager.getConnection(jdbcUrl, properties)
            createdConnections++
            KotlinLogging.logger {}.trace { "Got a connection currently at $createdConnections connections" }
            return r
        } catch (e: PSQLException) {
            println()
            println("Can't connect to PostGIS")
            e.printStackTrace()
            exitProcess(1)
        }
    }

    fun createWrappedConnection(config: Config) = ConnectionWrapper(createConnection(config))

}
