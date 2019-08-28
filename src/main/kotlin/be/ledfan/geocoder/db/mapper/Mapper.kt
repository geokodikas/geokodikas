package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.EntityCompanion
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.getLayer
import java.sql.PreparedStatement

abstract class Mapper<T>(private val con: ConnectionWrapper) {

    abstract val tableName: String

    abstract val entityCompanion: EntityCompanion<T>

    fun getByPrimaryId(id: Long): T? {
        val stmt = con.prepareStatement("SELECT *  FROM $tableName WHERE osm_id = ?") // TODO replace osm_id
        stmt.setLong(1, id)
        val result = stmt.executeQuery()

        if (!result.next()) {
            return null
        }

        val rEntity = entityCompanion.fillFromRow(result)

        if (result.next()) {
            throw RuntimeException("More than one result found for PK=$id.")
        }
        stmt.close()
        result.close()
        return rEntity

    }

    fun getByPrimaryIds(ids: List<Long>): HashMap<Long, T> {
        val sql = "SELECT * FROM $tableName WHERE osm_id = ANY(?)" // TODO replace osm_id
        val array = con.createArrayOf("BIGINT", ids.toTypedArray())
        val stmt = con.prepareStatement(sql)
        stmt.setArray(1, array)
        return executeSelect(stmt)
    }

    fun deleteByPrimaryId(id: Long) {
        val sql = "DELETE FROM $tableName WHERE osm_id = ?"
        val stmt = con.prepareStatement(sql) // TODO replace osm_id
        stmt.setLong(1, id)
        stmt.executeUpdate()
    }

    fun deleteByPrimaryIds(ids: List<Long>) {
        val sql = "DELETE  FROM $tableName WHERE osm_id = ANY(?)"
        val array = con.createArrayOf("BIGINT", ids.toTypedArray())
        val stmt = con.prepareStatement(sql) // TODO replace osm_id
        stmt.setArray(1, array)
        stmt.executeUpdate()
    }

    fun getAll(): HashMap<Long, T> {
        val sql = "SELECT * FROM $tableName WHERE"
        val stmt = con.prepareStatement(sql)
        return executeSelect(stmt)
    }

    protected fun executeSelect(stmt: PreparedStatement): HashMap<Long, T> {
        val result = stmt.executeQuery()

        val r = HashMap<Long, T>()

        while (result.next()) {
            val rEntity = entityCompanion.fillFromRow(result)
            r[result.getLong("osm_id")] = rEntity
        }

        stmt.close()
        result.close()
        return r
    }

    protected fun executeGroupedSelect(stmt: PreparedStatement, ids: List<Long>, groupByColumn: String): HashMap<Long, ArrayList<T>> {
        val result = stmt.executeQuery()

        val r = HashMap<Long, ArrayList<T>>()

        ids.forEach { r[it] = ArrayList() }

        while (result.next()) {
            val rel = entityCompanion.fillFromRow(result)

            val childId = result.getLong(groupByColumn)
            r[childId]?.add(rel)
        }

        stmt.close()
        result.close()

        return r
    }

}