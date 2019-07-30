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