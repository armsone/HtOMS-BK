package com.htoms.brief.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * Swift의 JSONValue enum과 1:1 대응하는 유연한 JSON 트리 표현.
 * kotlinx JsonElement에서 수동 변환하므로 직렬화기 생성 대상이 아니다.
 */
sealed class JSONValue {
    data class Object(val value: Map<String, JSONValue>) : JSONValue()
    data class Array(val value: List<JSONValue>) : JSONValue()
    data class Str(val value: String) : JSONValue()
    data class Num(val value: Double) : JSONValue()
    data class Bool(val value: Boolean) : JSONValue()
    data object Null : JSONValue()

    val objectValue: Map<String, JSONValue>?
        get() = (this as? Object)?.value

    val arrayValue: List<JSONValue>?
        get() = (this as? Array)?.value

    val stringValue: String?
        get() = when (this) {
            is Str -> value
            is Num -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
            is Bool -> value.toString()
            else -> null
        }

    val doubleValue: Double?
        get() = when (this) {
            is Num -> value
            is Str -> value.replace(",", "").toDoubleOrNull()
            else -> null
        }

    companion object {
        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun parse(jsonString: String): JSONValue {
            val element = jsonParser.parseToJsonElement(jsonString)
            return fromJsonElement(element)
        }

        fun fromJsonElement(element: JsonElement): JSONValue {
            return when (element) {
                is JsonNull -> Null
                is JsonObject -> Object(element.mapValues { fromJsonElement(it.value) })
                is JsonArray -> Array(element.map { fromJsonElement(it) })
                is JsonPrimitive -> {
                    if (element.isString) {
                        Str(element.content)
                    } else {
                        element.booleanOrNull?.let { return Bool(it) }
                        element.doubleOrNull?.let { return Num(it) }
                        Str(element.content)
                    }
                }
            }
        }
    }
}
