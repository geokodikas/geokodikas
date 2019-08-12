package be.ledfan.geocoder.importer.pipeline

import kotlin.reflect.KFunction
import kotlin.reflect.full.functions

open class AbstractValidator {


    fun validate(): Boolean {
        val validators = getValidators()
        val totalValidators = validators.size

        for ((idx, validator) in validators.withIndex()) {
            val r = validator.call(this) as? Boolean ?: throw Exception("Expected valiator ${validator.name }to return Boolean")

            if (r) {
                println("[$idx/$totalValidators] ${validator.name} OK")
            } else {
                println("[$idx/$totalValidators] ${validator.name} ERROR")
            }
        }

        return true
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

    protected  fun selectString(query: String, columnName: String): ArrayList<String> {
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