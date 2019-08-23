package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.geocoding.SearchTable

class AddressIndexMapper(private val con: ConnectionWrapper) : Mapper<AddressIndex>(con) {

    fun bulkInsert(addressIndexes: List<AddressIndex>) {
        val stmt = con.prepareStatement(
                """INSERT INTO address_index (osm_id, osm_type, street_id, neighbourhood_id, localadmin_id, county_id, macroregion_id, country_id, housenumber) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")

        for (dbObject in addressIndexes) {
            stmt.run {
                setLong(1, dbObject.id)
                when (dbObject.osm_type) {
                    SearchTable.Node -> setString(2, "n")
                    SearchTable.Way -> setString(2, "w")
                    SearchTable.Relation -> setString(2, "r")
                    else -> {
                    }
                }
                val sid = dbObject.street_id
                if (sid != null) {
                    setLong(3, sid)
                } else {
                    setNull(3, java.sql.Types.INTEGER)
                }
                val nbid = dbObject.neighbourhood_id
                if (nbid != null) {
                    setLong(4, nbid)
                } else {
                    setNull(4, java.sql.Types.INTEGER)
                }
                setLong(5, dbObject.localadmin_id)
                val cid = dbObject.county_id
                if (cid != null) {
                    setLong(6, cid)
                } else {
                    setNull(6, java.sql.Types.INTEGER)
                }
                setLong(7, dbObject.macroregion_id)
                setLong(8, dbObject.country_id)
                setString(9, dbObject.housenumber)

                addBatch()
            }
        }

        stmt.executeBatch()
        stmt.close()
    }

    fun getByWays(ways: List<OsmWay>): HashMap<Long, ArrayList<AddressIndex>> {
        val ids = ways.map { it.id }
        val sql = "SELECT * FROM address_index WHERE street_id = ANY(?)"
        val stmt = con.prepareStatement(sql)
        val array = con.createArrayOf("BIGINT", ids.toTypedArray())
        stmt.setArray(1, array)

        return executeGroupedSelect(stmt, ids, "street_id")
    }


    override val tableName = "address_index"

    override val entityCompanion = AddressIndex.Companion

}
