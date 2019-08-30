package be.ledfan.geocoder.db.cached

import be.ledfan.geocoder.db.entity.OsmRelation
import be.ledfan.geocoder.db.mapper.OsmRelationMapper
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton!
 */
class CachedRelationMapper {

    private val relations = ConcurrentHashMap<Long, OsmRelation>()
    private val logger = KotlinLogging.logger {}

    fun getByPrimaryIds(osmRelationMapper: OsmRelationMapper, ids: List<Long>): HashMap<Long, OsmRelation> {
        val r = HashMap<Long, OsmRelation>()
        val idsToFetch = arrayListOf<Long>()

        for (id in ids) {
            with(relations[id]) {
                if (this != null) {
                    r[id] = this
                } else {
                    idsToFetch.add(id)
                }
            }
        }

        if (idsToFetch.isNotEmpty()) {
            val fetched = osmRelationMapper.getSlimByPrimaryIds(idsToFetch)
            relations.putAll(fetched)
            r.putAll(fetched)
        }

        logger.debug { "Cached relations: ${relations.keys}"}

        return r
    }


}