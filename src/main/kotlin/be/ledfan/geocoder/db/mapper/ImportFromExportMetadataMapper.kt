package be.ledfan.geocoder.db.mapper

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.entity.OsmNode
import be.ledfan.geocoder.importer.Layer
import org.intellij.lang.annotations.Language
import org.postgresql.util.PSQLException
import java.util.*

data class MetaData(val dateTime: Any, val md5sum: String, val fileUrl: String)

class ImportFromExportMetadataMapper(private val con: ConnectionWrapper) {

    private fun createTable() {
        @Language("SQL")
        val sql = """
        CREATE TABLE IF NOT EXISTS import_from_export_metadata (
            datetime TIMESTAMP WITH TIME ZONE,
            md5sum   VARCHAR(255),
            file_url TEXT
        );
        """
        val stmt = con.prepareStatement(sql)
        stmt.executeUpdate()
        stmt.close()
    }

    fun getLastImport(): MetaData? {
        val stmt = con.prepareStatement("SELECT * FROM import_from_export_metadata ORDER BY datetime DESC LIMIT 1")
        val result = try {
            stmt.executeQuery()
        } catch (e: PSQLException) {
            // assume the relation does not exist, hence no import was done
            return null
        }

        if (!result.next()) return null

        return MetaData(result.getDate("datetime"),
                result.getString("md5sum"),
                result.getString("file_url"))
    }


    fun insert(md5sum: String, fileUrl: String) {
        createTable()
        val stmt = con.prepareStatement("INSERT INTO import_from_export_metadata (datetime, md5sum, file_url) VALUES (current_timestamp, ?, ?)")
        stmt.setString(1, md5sum)
        stmt.setString(2, fileUrl)
        stmt.executeUpdate()
    }

}
