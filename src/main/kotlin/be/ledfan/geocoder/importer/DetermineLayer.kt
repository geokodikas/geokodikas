package be.ledfan.geocoder.importer

import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.importer.core.Tags
import mu.KotlinLogging

open class DetermineLayer {

    protected open val ignore = listOf<String>()

    protected open val prune = listOf<String>()

    protected val logger = KotlinLogging.logger {}

    protected fun assignLayer(currentLayers: HashSet<Layer>, newLayer: Layer) {
        if ((currentLayers.contains(Layer.Street) || currentLayers.contains(Layer.Junction) || currentLayers.contains(Layer.Link))
                && (newLayer == Layer.PhysicalTrafficFlow || newLayer == Layer.VirtualTrafficFlow)) {
            // Street, Link and Junction will implicit contains Physical and Virtual traffic flow
            return
        }

        if ((currentLayers.contains(Layer.Address) || currentLayers.contains(Layer.Venue))
                && (newLayer == Layer.PhysicalTrafficFlow || newLayer == Layer.VirtualTrafficFlow)) {
            // not interested in speed limits on addresses (buildings, parkings etc) and or venues
            return
        }

        if (currentLayers.contains(Layer.PhysicalTrafficFlow)
                && newLayer == Layer.VirtualTrafficFlow) {
            // PhysicalTrafficFlow implies VirtualTrafficFlow
            return
        }

        if (currentLayers.contains(Layer.VirtualTrafficFlow)
                && newLayer == Layer.PhysicalTrafficFlow) {
            // PhysicalTrafficFlow implies virtualTrafficFlow
            currentLayers.remove(Layer.VirtualTrafficFlow)
            currentLayers.add(Layer.PhysicalTrafficFlow)
            return
        }

        if ((currentLayers.contains(Layer.Street) || currentLayers.contains(Layer.Link))
                && (newLayer == Layer.Junction)) {
            // The fact that it is a Junction is more import than the fact that it is a Street
            currentLayers.remove(Layer.Street)
            currentLayers.remove(Layer.Link)
            currentLayers.add(newLayer)
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
