package be.ledfan.geocoder.importer.steps

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.importer.core.Importer
import be.ledfan.geocoder.kodein
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.kodein.di.direct
import org.kodein.di.generic.instance

private val con = kodein.direct.instance<ConnectionWrapper>()
private val logger = KotlinLogging.logger {}

fun countTable(tableName: String): Long {

    val stmt = con.prepareStatement("SELECT COUNT(*) as count FROM $tableName")
    val result = stmt.executeQuery()
    result.next()
    return result.getLong("count")
}


fun ensureNotEmpty(tableName: String): Boolean {

    val count = countTable(tableName)

    return if (count == 0L) {
        logger.error { "The upstream `$tableName` table is empty" }
        false
    } else {
        logger.debug { "Found $count `$tableName` records" }
        true
    }
}

fun ensureEmpty(tableName: String): Boolean {

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

suspend fun executeBatchQueriesParallel(sqlQueries: List<String>): Boolean {
    val jobs = ArrayList<Job>()
    for (query in sqlQueries) {
        jobs.add(GlobalScope.launch(Dispatchers.IO) {
            val logger = KotlinLogging.logger {}
            logger.info("Scheduling query to run in parallel")
            val stmt = con.prepareStatement(query)
            stmt.executeUpdate()
            stmt.close()
            logger.info("Query was executed")
        })
    }
    jobs.joinAll()
    return true
}
