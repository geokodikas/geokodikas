package be.ledfan.geocoder.db

import be.ledfan.geocoder.config.Config
import be.ledfan.geocoder.kodein
import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.sql.CallableStatement
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.*

class ConnectionWrapper(var connection: Connection) {

    fun prepareStatement(s: String): PreparedStatement = connection.prepareStatement(s)

    fun createArrayOf(s: String, o: Array<Any>): java.sql.Array? = connection.createArrayOf(s, o)

    fun prepareCall(s: String): CallableStatement = connection.prepareCall(s)

    fun close() = connection.close()

    fun reConnect() {
        if (!connection.isClosed) {
            connection.close()
        }

        connection = ConnectionFactory.createConnection(kodein.direct.instance())
    }

}