package be.ledfan.geocoder.importer.core

import de.topobyte.osm4j.core.model.iface.OsmEntity

import be.ledfan.geocoder.db.entity.OsmEntity as dbOsmEntity

fun OsmEntity.copyVersionAndTags(into: dbOsmEntity) {
    metadata?.version?.let {
        into.version = it
    }

    if (numberOfTags > 0) {
        for (i in 0 until numberOfTags) {
            into.tags[getTag(i).key] = getTag(i).value
        }
    }
}

fun <T> List<T>.hasAtLeast(amount: Int, predicate: (T) -> Boolean): Boolean {
    var cnt = 0
    for (el in this) {
        if (predicate(el)) {
            cnt++
        }
        if (cnt == amount) {
            return true
        }
    }
    return false
}