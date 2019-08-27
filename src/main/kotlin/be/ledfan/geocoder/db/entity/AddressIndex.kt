package be.ledfan.geocoder.db.entity

import be.ledfan.geocoder.geocoding.SearchTable
import be.ledfan.geocoder.importer.Layer
import java.sql.ResultSet

class AddressIndex(val id: Long) : Entity {

    companion object : EntityCompanion<AddressIndex> {

        override fun fillFromRow(row: ResultSet): AddressIndex {
            val r = AddressIndex(row.getLong("osm_id"))

            when (row.getString("osm_type")) {
                "node" -> r.osm_type = SearchTable.Node
                "way" -> r.osm_type = SearchTable.Way
                "relation" -> r.osm_type = SearchTable.Relation
            }

            r.street_id = row.getLong("street_id")
            r.neighbourhood_id = row.getLong("neighbourhood_id")
            r.localadmin_id = row.getLong("localadmin_id")
            r.county_id = row.getLong("county_id")
            r.macroregion_id = row.getLong("macroregion_id")
            r.country_id = row.getLong("country_id")
            r.housenumber = row.getString("housenumber")

            return r
        }

        fun create(osmId: Long, osmType: SearchTable): AddressIndex {
            val r = AddressIndex(osmId)
            r.osm_type = osmType
            return r
        }

    }

    lateinit var osm_type: SearchTable
    var housenumber: String? = null
    var street_id: Long? = null
    var neighbourhood_id: Long? = null
    var localadmin_id: Long = 0
    var county_id: Long? = null
    var macroregion_id: Long = 0
    var country_id: Long = 0

}