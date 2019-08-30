package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.cached.CachedRelationMapper
import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.db.entity.OsmType
import java.sql.Types

class AddressIndexMapper(private val con: ConnectionWrapper,
                         private val osmWayMapper: OsmWayMapper,
                         private val osmRelationMapper: OsmRelationMapper,
                         private val cachedRelationMapper: CachedRelationMapper) : Mapper<AddressIndex>(con) {

    override val tableName = "address_index"

    override val entityCompanion = AddressIndex.Companion

    fun bulkInsert(addressIndexes: List<AddressIndex>) {
        val stmt = con.prepareStatement(
                """INSERT INTO address_index (osm_id, osm_type, street_id, neighbourhood_id, localadmin_id, county_id, macroregion_id, country_id, housenumber, geometry, tags, layer)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::Layer)""")

        for (dbObject in addressIndexes) {
            stmt.run {
                setLong(1, dbObject.id)
                when (dbObject.osmType) {
                    OsmType.Node -> setString(2, "node")
                    OsmType.Way -> setString(2, "way")
                    OsmType.Relation -> setString(2, "relation")
                    else -> {
                    }
                }
                with(dbObject.streetId) {
                    if (this != null) {
                        setLong(3, this)
                    } else {
                        setNull(3, Types.INTEGER)
                    }
                }
                with(dbObject.neighbourhoodId) {
                    if (this != null) {
                        setLong(4, this)
                    } else {
                        setNull(4, Types.INTEGER)
                    }
                }
                setLong(5, dbObject.localadminId)
                with(dbObject.countyId) {
                    if (this != null) {
                        setLong(6, this)
                    } else {
                        setNull(6, Types.INTEGER)
                    }
                }
                setLong(7, dbObject.macroregionId)
                setLong(8, dbObject.countryId)
                setString(9, dbObject.housenumber)
                setObject(10, dbObject.geometry)
                setObject(11, dbObject.tags)
                setString(12, dbObject.layer.name)

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


    fun fetchRelations(addressIndex: AddressIndex) {
        val relationIds = arrayListOf<Long>(
                addressIndex.countryId,
                addressIndex.localadminId,
                addressIndex.macroregionId)

        addressIndex.countyId?.let { relationIds.add(it) }
        addressIndex.neighbourhoodId?.let { relationIds.add(it) }

        val relations = cachedRelationMapper.getByPrimaryIds(osmRelationMapper, relationIds)

        addressIndex.country = relations[addressIndex.countryId]
        addressIndex.localAdmin = relations[addressIndex.localadminId]
        addressIndex.macroregion = relations[addressIndex.macroregionId]
        addressIndex.countyId?.let { addressIndex.county = relations[it] }
        addressIndex.neighbourhoodId?.let { addressIndex.neighbourhood = relations[it] }
        addressIndex.streetId?.let { addressIndex.street = osmWayMapper.getByPrimaryId(it) }
        addressIndex.entity = osmWayMapper.getByPrimaryId(addressIndex.id)  // TODO combine with query above ^

        addressIndex.relationsFetched = true
    }


}
