package be.ledfan.geocoder.db.entity

//import be.ledfan.geocoder.geocoder.INameResolvable
//import be.ledfan.geocoder.geocoder.IRetrievable
//import be.ledfan.geocoder.geocoder.ITagParsable
import be.ledfan.geocoder.importer.Layer

open class OsmEntity(val id: Long) : Entity {

    var tags: HashMap<String, String> = HashMap()
    var zOrder: Int = 0
    lateinit var layer: Layer

    var version: Int = 0

    fun assignLayer(newLayer: Layer) {
        if (::layer.isInitialized && layer != newLayer) {
            throw Exception("Layer of Node $id already assigned to $layer, trying to assign it to $newLayer")
        }
        layer = newLayer
    }

}