package be.ledfan.geocoder.db.entity

//import be.ledfan.geocoder.geocoder.INameResolvable
//import be.ledfan.geocoder.geocoder.IRetrievable
//import be.ledfan.geocoder.geocoder.ITagParsable
import be.ledfan.geocoder.importer.Layer

abstract class OsmEntity(val id: Long) : Entity {

    abstract val Type: OsmType

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

    val dynamicProperties = HashMap<String, Any>()

}