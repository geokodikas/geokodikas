package be.ledfan.geocoder.importer.steps

import be.ledfan.geocoder.db.ConnectionFactory
import mu.KotlinLogging

private val con = ConnectionFactory.createConnection()
private val logger = KotlinLogging.logger {}

fun countTable(tableName: String): Long {

    val stmt = con.prepareStatement("SELECT COUNT(*) as count FROM $tableName")
    val result = stmt.executeQuery()
    result.next()
    return result.getLong("count")
}


fun ensureNotEmpty(tableName: String) : Boolean {

    val count = countTable(tableName)

    return if (count == 0L) {
        logger.error { "The upstream `$tableName` table is empty" }
        false
    } else {
        logger.debug { "Found $count `$tableName` records" }
        true
    }
}

fun ensureEmpty(tableName: String) : Boolean {

    val count = countTable(tableName)

    return if (count != 0L) {
        logger.error { "The local `$tableName` table is not empty" }
        false
    } else {
        logger.debug { "Found $count `$tableName` records (OK)" }
        true
    }
}

fun executeBatchQueries(sqlQueries: List<String>): Boolean {
    for (query in sqlQueries) {
        val stmt = con.prepareStatement(query)
        stmt.executeUpdate()
        stmt.close()
    }

    return true
}

