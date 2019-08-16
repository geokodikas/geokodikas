package be.ledfan.geocoder.db

import java.sql.ResultSet

@Suppress("UNCHECKED_CAST")
fun ResultSet.getHstore(key: String): HashMap<String, String> {
    return getObject(key) as HashMap<String, String>
}