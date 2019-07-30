//package be.ledfan.geocoder.importer
//
////import be.ledfan.geocoder.geocoder.ITagParsable
//import java.lang.Exception
//
//class Tags(var values: List<String>?) {
//
//    constructor() : this(null)
//
//    constructor(value: String) : this() {
//        values = value.split(";")
//    }
//
//    val children: HashMap<String, Tags> = HashMap()
//
//    val keys: Set<String>
//        get() = children.keys
//
//    fun setValue(key: String, value: String) {
//        val child = children[key]
//        if (child != null) {
//            if (child.values != null) {
//                throw Exception("Duplicate key!")
//            } else {
//                child.values = value.split(";")
//            }
//        } else {
//            children[key] = Tags(value)
//        }
//    }
//
//    fun child(key: String): Tags {
//        val child = children[key]
//        if (child != null) {
//            return child
//        } else {
//            val r = Tags()
//            children[key] = r
//            return r
//        }
//    }
//
//    fun deepChild(keys: List<String>): Tags {
//        var current = this
//        for (key in keys) {
//            current = current.child(key)
//        }
//        return current
//    }
//
//    fun setDeepValue(keys: List<String>, value: String) {
//        deepChild(keys).values = value.split(";")
//    }
//
//    fun hasChild(key: String): Boolean {
//        return children.containsKey(key)
//    }
//
//    fun getSingleValueOfChild(key: String): String? {
//        if (children[key]?.values?.size == 1) {
//            return children[key]!!.values!![0]
//        }
//        return null
//    }
//
//    fun hasChild(vararg keys: String): Boolean {
//        for (key in keys) {
//            if (children.containsKey(key)) {
//                return true
//            }
//        }
//        return false
//    }
//
//    fun toString(nesting: Int = 0): String {
//        var r = ""
//        if (values != null) {
//            r += " => ${values?.joinToString()}\n"
//        } else if (nesting != 0) {
//            r += " --> \n"
//        }
//        children.forEach {
//            r += "\t".repeat(nesting) + " ${it.key}"
//            r += it.value.toString(nesting + 1)
//        }
//        return r
//    }
//
//    fun size(): Int {
//        val r = children.size
//        if (values != null) {
//            return r + 1
//        }
//        return r
//    }
//
//    fun letChild(key: String, block: (Tags) -> Unit) {
//        val child = children[key]
//        if (child != null) {
//            block(child)
//        }
//    }
//
//    fun letChild(vararg keys: String, block: (Tags) -> Unit) {
//        for (key in keys) {
//            val child = children[key]
//            if (child != null) {
//                block(child)
//                break
//            }
//        }
//    }
//
//    fun hasValue(vararg keys: String): Boolean {
//        val b = values?.any { keys.contains(it) } // if values contains a key from keys
//        if (b != null && b) {
//            return true
//        }
//        return false
//    }
//
//    fun hasValue(value: String): Boolean {
//        return values?.contains(value) ?: false
//    }
//}
//
//class TagParser {
//
//    fun parse(node: ITagParsable): Tags {
//
//        val result = Tags()
//
//        if (node.tags.size == 0) {
//            return result
//        }
//
//        for ((key, value) in node.tags) {
//            val keys = key.split(':')
//
//            if (keys.isNotEmpty()) {
//                result.setDeepValue(keys, value)
//            } else {
//                result.setValue(key, value)
//            }
//
//        }
//
//        return result
//
//    }
//
//}