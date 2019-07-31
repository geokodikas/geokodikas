package be.ledfan.geocoder.importer.processors

import be.ledfan.geocoder.db.mapper.OsmRelationMapper
import be.ledfan.geocoder.db.mapper.OsmUpstreamPolygonMapper
import be.ledfan.geocoder.importer.DetermineLayerRelation
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.core.BaseProcessor
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.importer.core.copyVersionAndTags
import de.topobyte.osm4j.core.model.iface.OsmRelation
import mu.KotlinLogging
import be.ledfan.geocoder.db.entity.OsmRelation as dbOsmRelation

/**
 * Process OsmRelation objects.
 */
class OsmRelationProcessor(private var determineLayer: DetermineLayerRelation,
                           private var tagParser: TagParser,
                           private val osmRelationMapper: OsmRelationMapper,
                           private val osmUpstreamRelationMapper: OsmUpstreamPolygonMapper) : BaseProcessor<OsmRelation>() {

    private var logger = KotlinLogging.logger {}

    override suspend fun processEntities(entities: List<OsmRelation>) {
        val dbObjects: ArrayList<dbOsmRelation> = ArrayList()

        val osmIds = entities.map { -it.id } // negative because we are searching for relations

        val upstreamObjects = osmUpstreamRelationMapper.getByPrimaryIds(osmIds)

        loop@ for (entity in entities) {
            val upPolygon = upstreamObjects[-entity.id]

            if (upPolygon == null) {
                logger.trace { "Skipping ${entity.id} because no upstream object found" }
                continue
            }

            val dbObject = dbOsmRelation(entity.id)
            entity.copyVersionAndTags(dbObject)

            dbObject.geometry = upPolygon.way

            // first determine Layer
            val tags = tagParser.parse(dbObject.tags)
            val layers = determineLayer.determine(dbObject, tags)

            layers.remove(Layer.Superfluous) // Remove superfluous layers, if layers left -> not superfluous

            when (layers.size) {
                0 -> logger.trace { "Skipping ${dbObject.id} because no suitable layer found, tags: ${tags.toString(0)}" }
                1 -> {
                    dbObject.assignLayer(layers.first())
                    val name = tags.childOrNull("name")?.singleValueOrNull()
                    if (name != null) {
                        dbObject.name = name
                    } else {
                        logger.trace { "No nameTag found for relation ${dbObject.id}" }
                    }

                    dbObjects.add(dbObject)
                }
                else -> throw Exception("Multiple layer found for ${dbObject.id} this should NOT Happen. Relation will not be imported. Layers=${layers.sorted().joinToString()}")
            }

        }
        osmRelationMapper.bulkInsert(dbObjects)
    }
}
