package be.ledfan.geocoder.importer

import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.importer.core.Tags
import mu.KotlinLogging

open class DetermineLayer {

    protected open val ignore = listOf<String>()

    protected open val prune = listOf<String>()

    protected val logger = KotlinLogging.logger {}

    protected fun assignLayer(currentLayers: HashSet<Layer>, newLayer: Layer) {
        fun resolveableConflict(loosingLayers: List<Layer>, winningLayer: List<Layer>): Boolean {
            if (
                    currentLayers.any { loosingLayers.contains(it) } // if we currently contain a loosing layer
                    && winningLayer.contains(newLayer) // and the newLayer is a winning layer
            ) {
                currentLayers.removeAll(loosingLayers) // remove all
                return true // conflict resolved, layer will be added later
            }

            if (currentLayers.any { winningLayer.contains(it) } // if we currently contain a winning layer
                    && loosingLayers.contains(newLayer)) { // and the newLayer is a loosing layer
                // then not interested in new layer
                return false
            }

            return true // interested in this layer
        }

        if (!resolveableConflict(listOf(Layer.Street, Layer.Junction, Layer.Link),
                        listOf(Layer.PhysicalTrafficFlow, Layer.VirtualTrafficFlow))) {
            // Street, Link and Junction will implicit contains Physical and Virtual traffic flow
            return
        }

        if (!resolveableConflict(listOf(Layer.Address, Layer.Venue),
                        listOf(Layer.PhysicalTrafficFlow, Layer.VirtualTrafficFlow))) {
            // not interested in speed limits on addresses (buildings, parkings etc) and or venues
            return
        }

        if (!resolveableConflict(listOf(Layer.PhysicalTrafficFlow), listOf(Layer.VirtualTrafficFlow))) {
            // PhysicalTrafficFlow implies VirtualTrafficFlow
            return
        }

        if (!resolveableConflict(listOf(Layer.Street, Layer.Link), listOf(Layer.Junction))) {
            // The fact that it is a Junction is more import than the fact that it is a Street
            return
        }

        if (!currentLayers.contains(newLayer)) {
            currentLayers.add(newLayer)
        }
    }


    protected fun determineHelper(entity: OsmEntity, parsedTags: Tags, specificDeterminer: (HashSet<Layer>) -> Unit): HashSet<Layer> {
        val layers = HashSet<Layer>()

        if (parsedTags.size() == 0) {
            // fallback to Superfluous, since it has no tags
            // if this was a junction it would have had at least two linked ways
            // and not reach this part of the code
            assignLayer(layers, Layer.Superfluous)
            return layers
        }

        if (parsedTags.hasAnyChild(prune)) {
            assignLayer(layers, Layer.Superfluous)
            return layers
        }

        if (parsedTags.hasAnyChild(ignore)) {
            assignLayer(layers, Layer.Superfluous)
        }

        specificDeterminer(layers)

        if (layers.size == 0) {
            logger.warn { "LAYER ${entity.id} is unknown: ${parsedTags.toString(0).trim()}" }
            assignLayer(layers, Layer.Superfluous)
        }

        return layers
    }


}
