@file:Suppress("FunctionName")

package be.ledfan.geocoder.importer.steps

import org.intellij.lang.annotations.Language

suspend fun drop_tables(): Boolean {

    @Language("SQL")
    val sqlQueries = listOf(
            "DROP TABLE IF EXISTS osm_node",
            "DROP TABLE IF EXISTS osm_way",
            "DROP TABLE IF EXISTS way_node",
            "DROP TABLE IF EXISTS osm_relation",
            "DROP TABLE IF EXISTS parent")

    return executeBatchQueries(sqlQueries)
}