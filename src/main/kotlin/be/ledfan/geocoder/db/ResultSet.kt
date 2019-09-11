package be.ledfan.geocoder.db

import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.entity.OsmWay
import be.ledfan.geocoder.importer.Layer
import org.postgis.PGgeometry
import org.postgresql.util.PSQLException
import java.sql.ResultSet

@Suppress("UNCHECKED_CAST")
fun ResultSet.getHstore(key: String): HashMap<String, String> {
    return getObject(key) as HashMap<String, String>
}

fun ResultSet.getLayer(key: String = "layer"): Layer {
    return Layer.valueOf(getString(key))
}

fun ResultSet.getGeometry(r: OsmWay) {
    try {
        r.geometry = getObject("geometry") as PGgeometry
    } catch (e: PSQLException) {
        // geometry was not fetched
    }
}

fun ResultSet.getGeometry(r: AddressIndex) {
    try {
        r.geometry = getObject("geometry") as PGgeometry
    } catch (e: PSQLException) {
        // geometry was not fetched
    }
}

fun ResultSet.getGeometry(r: OsmRelation) {
    try {
        r.geometry = getObject("geometry") as PGgeometry
    } catch (e: PSQLException) {
        // geometry was not fetched
    }
}

fun ResultSet.getCentroid(r: OsmWay) {
    try {
        r.centroid = getObject("centroid") as PGgeometry
    } catch (e: PSQLException) {
        // geometry was not fetched
    }
}

fun ResultSet.getCentroid(r: OsmNode) {
    try {
        r.centroid = getObject("centroid") as PGgeometry
    } catch (e: PSQLException) {
        // geometry was not fetched
    }
}
