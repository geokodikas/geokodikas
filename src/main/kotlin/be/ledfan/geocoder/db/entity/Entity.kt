package be.ledfan.geocoder.db.entity

import java.sql.ResultSet

interface EntityCompanion<T> {

    fun fillFromRow(row: ResultSet): T

}

interface Entity
