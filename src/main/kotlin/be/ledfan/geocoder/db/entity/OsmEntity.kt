package be.ledfan.geocoder.db.entity

//import be.ledfan.geocoder.geocoder.INameResolvable
//import be.ledfan.geocoder.geocoder.IRetrievable
//import be.ledfan.geocoder.geocoder.ITagParsable
import be.ledfan.geocoder.importer.Layer
import be.ledfan.geocoder.importer.core.TagParser
import be.ledfan.geocoder.importer.core.Tags
import org.postgis.PGgeometry

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

    abstract fun mainGeometry(): PGgeometry

    val dynamicProperties = HashMap<String, Any>()

    val parsedTags: Tags by lazy {
        println("Parsing tags for $id")
        TagParser().parse(tags)
    }

}