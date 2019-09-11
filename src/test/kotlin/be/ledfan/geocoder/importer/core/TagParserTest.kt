package be.ledfan.geocoder.importer.core

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TagParserUnitTest {

    @Test
    fun complex_parse_test() {
        val tagParser = TagParser()

        val input1 = mapOf(
                "surface" to "asphalt",
                "maxspeed" to "30",
                "maxspeed:type" to "BE:zone30",
                "lit" to "yes",
                "highway" to "residential",
                "source:maxspeed" to "BE:zone30",
                "name" to "Avenue Jeanne - Johannalaan",
                "name:nl" to "Johannalaan",
                "name:fr" to "Avenue Jeanne",
                "multival" to "val1;val2;val3",
                "very" to "yes",
                "very:very_very" to "yes",
                "very:very_very:deeply" to "yes",
                "very:very_very:deeply:nested" to "yes",
                "nested:without:value" to "val1;val2;val5",
                "nested:without:value2" to "test"
        )

        val actual = tagParser.parse(input1)
        val expected = Tags()
        expected.createChild("surface").setValues("asphalt")
        expected.createChild("maxspeed").setValues("30")
        expected.createChild("maxspeed").createChild("type").setValues("BE:zone30")
        expected.createChild("lit").setValues("yes")
        expected.createChild("source").createChild("maxspeed").setValues("BE:zone30")
        expected.createChild("name").setValues("Avenue Jeanne - Johannalaan")
        expected.createChild("name").createChild("nl").setValues("Johannalaan")
        expected.createChild("name").createChild("fr").setValues("Avenue Jeanne")
        expected.createChild("multival").setValues("val1;val2;val3")
        expected.createChild("very").setValues("yes")
        expected.createChild("highway").setValues("residential")
        expected.child("very").createChild("very_very").setValues("yes")
        expected.child("very").child("very_very").createChild("deeply").setValues("yes")
        expected.child("very").child("very_very").child("deeply").createChild("nested").setValues("yes")
        expected.createDescendant(listOf("nested", "without", "value")).setValues("val1;val2;val5")
        expected.createDescendant(listOf("nested", "without", "value2")).setValues("test")

        assertEquals(actual, expected)
        assertTrue { expected.child("multival").hasValue("val1") }
        assertTrue { expected.child("multival").hasValue("val2") }
        assertTrue { expected.child("multival").hasValue("val3") }
        expected.child("nested").child("without").child("value").let {
            assertTrue { it.hasValue("val1") }
            assertTrue { it.hasValue("val2") }
            assertTrue { it.hasValue("val5") }
            assertTrue { it.hasAnyValue(listOf("val1", "val2", "val42"))}
        }

        assertEquals(null, expected.descendant(listOf("nested", "without", "value")).singleValueOrNull())
        assertEquals("test", expected.descendant(listOf("nested", "without", "value2")).singleValueOrNull())

        assertEquals("""
 multival => val1, val2, val3
 very => yes
	 very_very => yes
		 deeply => yes
			 nested => yes
 surface => asphalt
 lit => yes
 maxspeed => 30
	 type => BE:zone30
 name => Avenue Jeanne - Johannalaan
	 fr => Avenue Jeanne
	 nl => Johannalaan
 source --> 
	 maxspeed => BE:zone30
 highway => residential
 nested --> 
	 without --> 
		 value2 => test
		 value => val1, val2, val5
""".trim(), expected.toString(0).trim())

        assertEquals(9, actual.size())

    }

    @Test
    fun set_values() {
        val tags = Tags()
        tags.setValues("test")

        val exception = assertThrows(Exception::class.java) { tags.setValues("test") }
        assertEquals("Already contains a value", exception.message)
    }

    @Test
    fun no_child() {
        val tags = Tags()

        val exception = assertThrows(Exception::class.java) { tags.child("test") }
        assertEquals("No such child with key 'test'", exception.message)

        assertNull(tags.childOrNull("test"))

        assertFalse(tags.hasAnyChild(listOf("test", "abc", "123")))

        tags.createChild("abc")
        assertTrue(tags.hasAnyChild(listOf("test", "abc", "123")))
        tags.child("abc").createChild("nested")
        assertFalse(tags.hasChild("nested"))
    }

    @Test
    fun size() {
        val tags = Tags()
        tags.setValues("test")
        assertEquals(1, tags.size())

    }

    @Test
    fun parse_empty() {
        val tagParser = TagParser()
        assertEquals(0, tagParser.parse(mapOf()).size())
    }

}