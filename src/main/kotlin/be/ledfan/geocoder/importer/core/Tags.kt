package be.ledfan.geocoder.importer.core

import java.lang.Exception

data class Tags(var values: List<String>? = null, val children: HashMap<String, Tags> = HashMap()) {

    val amountOfChildren: Int
        get() = children.size

    fun setValues(value: String) {
        if (values != null) {
            throw Exception("Already contains a value")
        } else {
            values = value.split(";")
        }
    }

    fun singleValueOrNull(): String? {
        return if (values?.size == 1) {
            values?.get(0)
        } else {
            null
        }
    }

    fun child(key: String): Tags {
        val child = children[key]
        if (child != null) {
            return child
        } else {
            throw Exception("No such child with key '$key'")
        }
    }

    fun childOrNull(key: String): Tags? {
        return children[key]
    }

    fun hasChild(key: String): Boolean {
        return children.containsKey(key)
    }

    fun hasAnyChild(keys: List<String>): Boolean {
        return children.keys.any { keys.contains(it) }
    }

    fun createChild(key: String): Tags {
        if (!hasChild(key)) {
            children[key] = Tags()
        }

        return child(key)
    }

    fun descendant(keys: List<String>): Tags {
        var current = this
        for (key in keys) {
            current = current.child(key)
        }
        return current
    }

    fun createDescendant(keys: List<String>): Tags {
        var current = this
        for (currentKey in keys) {
            if (!current.hasChild(currentKey)) {
                current.children[currentKey] = Tags()
            }
            current = current.child(currentKey)
        }
        return current
    }

    fun toString(nesting: Int = 0): String {
        var r = ""
        if (values != null) {
            r += " => ${values?.joinToString()}\n"
        } else if (nesting != 0) {
            r += " --> \n"
        }
        children.forEach {
            r += "\t".repeat(nesting) + " ${it.key}"
            r += it.value.toString(nesting + 1)
        }
        return r
    }

    fun size(): Int {
        val r = children.size
        if (values != null) {
            return r + 1
        }
        return r
    }

    fun hasValue(value: String): Boolean {
        return values?.contains(value) ?: false
    }

    fun hasAnyValue(values: List<String>): Boolean {
        return this.values?.any { values.contains(it) } ?: false
    }

}