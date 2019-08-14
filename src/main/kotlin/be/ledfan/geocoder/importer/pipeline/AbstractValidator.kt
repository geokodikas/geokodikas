package be.ledfan.geocoder.importer.pipeline

import be.ledfan.geocoder.db.ConnectionWrapper
import be.ledfan.geocoder.db.mapper.OsmRelationMapper
import be.ledfan.geocoder.kodein
import mu.KotlinLogging
import org.kodein.di.direct
import org.kodein.di.generic.instance
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions

class ValidationException(s: String) : java.lang.Exception(s)

@ExperimentalContracts
internal fun assertNull(actual: Any?) {
    contract { returns() implies (actual == null) }
    if (actual == null) {
        throw ValidationException("Assertion null failed")
    }
}

@ExperimentalContracts
internal fun assertNotNull(actual: Any?) {
    contract { returns() implies (actual != null) }
    if (actual == null) {
        throw ValidationException("Assertion not null failed")
    }
}

@ExperimentalContracts
internal fun assertTrue(actual: Boolean) {
    contract { returns() implies (actual) }
    if (!actual) {
        throw ValidationException("Assertion true failed")
    }
}

@ExperimentalContracts
internal fun assertFalse(actual: Boolean) {
    contract { returns() implies (!actual) }
    if (actual) {
        throw ValidationException("Assertion false failed")
    }
}

@ExperimentalContracts
internal fun assertEquals(expected: Any, actual: Any?) {
    assertTrue(expected == actual)
}

open class AbstractValidator {

    protected var relationMapper: OsmRelationMapper = kodein.direct.instance()
    private var con: ConnectionWrapper = kodein.direct.instance()
    private var logger = KotlinLogging.logger {}

    fun validate(): Boolean {
        val validators = getValidators()
        val totalValidators = validators.size
        var failure = false

        for ((idx, validator) in validators.withIndex()) {
            try {
                validator.call(this)
                logger.info { "[$idx/$totalValidators] ${validator.name} OK" }
            } catch (e: ValidationException) {
                logger.error { "[$idx/$totalValidators] ${validator.name} ERROR" }
                failure = true
            }
        }

        return failure
    }


    private fun getValidators(): ArrayList<KFunction<*>> {
        val validators = ArrayList<KFunction<*>>()
        val declaredMethods = this::class.functions

        for (method in declaredMethods) {
            for (annotation in method.annotations) {
                if (annotation is Validator) {
                    validators.add(method)
                }
            }
        }

        return validators
    }

    @Target(AnnotationTarget.FUNCTION)
    annotation class Validator

    protected fun selectIds(query: String): ArrayList<Long> {
//        val stmt = con.prepareStatement(query)
//        val result = stmt.executeQuery()
//
        val ids = arrayListOf<Long>()
//
//        while (result.next()) {
//            ids.add(result.getLong("osm_id"))
//        }
//
//        stmt.close()
//        result.close()
//
        return ids
    }

    protected fun selectString(query: String, columnName: String): ArrayList<String> {
//        val stmt = con.prepareStatement(query)
//        val result = stmt.executeQuery()
//
        val strings = arrayListOf<String>()
//
//        while (result.next()) {
//            strings.add(result.getString(columnName))
//        }
//
//        stmt.close()
//        result.close()
//
        return strings

    }

}