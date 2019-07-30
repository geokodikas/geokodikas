package be.ledfan.geocoder.db.entity

import java.sql.ResultSet

interface EntityCompanion {

    fun fillFromRow(row: ResultSet): Entity

}

interface Entity