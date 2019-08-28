package be.ledfan.geocoder.db.entity

import java.sql.ResultSet

class AddressIndex(val id: Long) : Entity {

    val Type = OsmType.AddressIndex

    companion object : EntityCompanion<AddressIndex> {

        override fun fillFromRow(row: ResultSet): AddressIndex {
            val r = AddressIndex(row.getLong("osm_id"))

            when (row.getString("osm_type")) {
                "node" -> r.osmType = OsmType.Node
                "way" -> r.osmType = OsmType.Way
                "relation" -> r.osmType = OsmType.Relation
            }

            r.streetId = row.getLong("street_id")
            r.neighbourhoodId = row.getLong("neighbourhood_id")
            r.localadminId = row.getLong("localadmin_id")
            r.countyId = row.getLong("county_id")
            r.macroregionId = row.getLong("macroregion_id")
            r.countryId = row.getLong("country_id")
            r.housenumber = row.getString("housenumber")

            return r
        }

        fun create(osmId: Long, osmType: OsmType): AddressIndex {
            val r = AddressIndex(osmId)
            r.osmType = osmType
            return r
        }

    }

    var entity: OsmWay? = null

    lateinit var osmType: OsmType
    var housenumber: String? = null

    var streetId: Long? = null
    var street: OsmWay? = null // TODO this could be a relation in theory (hence remove the code)

    var neighbourhoodId: Long? = null
    var neighbourhood: OsmRelation? = null

    var localadminId: Long = 0
    var localAdmin: OsmRelation? = null

    var countyId: Long? = null
    var county: OsmRelation? = null

    var macroregionId: Long = 0
    var macroregion: OsmRelation? = null

    var countryId: Long = 0
    var country: OsmRelation? = null

    var relationsFetched = false

}