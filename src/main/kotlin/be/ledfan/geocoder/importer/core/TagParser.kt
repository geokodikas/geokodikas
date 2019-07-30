package be.ledfan.geocoder.importer.core

class TagParser {

    fun parse(tags: Map<String, String>): Tags {
        val result = Tags()

        if (tags.isEmpty()) {
            return result
        }

        for ((key, value) in tags) {
            val keys = key.split(':')
            result.createDescendant(keys).setValues(value)
        }

        return result
    }

}