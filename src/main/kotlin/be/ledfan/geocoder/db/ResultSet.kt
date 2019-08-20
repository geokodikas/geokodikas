package be.ledfan.geocoder.db

import be.ledfan.geocoder.importer.Layer
import java.sql.ResultSet

@Suppress("UNCHECKED_CAST")
fun ResultSet.getHstore(key: String): HashMap<String, String> {
    return getObject(key) as HashMap<String, String>
}

fun ResultSet.getLayer(key: String = "layer"): Layer {
    return Layer.valueOf(getString(key))
}
