package com.navvistest.mobilechallenge.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Decodes a response body like `{"numbers": [4, 150, null]}`.
 *
 * Elements are decoded one at a time, so a single bad value becomes [RawValue.Invalid]
 * and the rest of the chunk is kept as is. Only problems with the body as a whole
 * (not JSON, missing key, not an array) can fail a request.
 */
object ChunkDecoder {

    fun decode(body: String, key: String): ApiResult<List<RawValue>> {
        val root = try {
            Json.parseToJsonElement(body)
        } catch (e: SerializationException) {
            return ApiResult.Failure(ApiError.MalformedBody("Body is not valid JSON"))
        }
        if (root !is JsonObject) {
            return ApiResult.Failure(ApiError.MalformedBody("Body is not a JSON object"))
        }
        val array = when (val value = root[key]) {
            null -> return ApiResult.Failure(ApiError.MalformedBody("Missing key \"$key\""))
            !is JsonArray -> return ApiResult.Failure(ApiError.MalformedBody("\"$key\" is not an array"))
            else -> value
        }
        return ApiResult.Success(array.map(::toRawValue))
    }

    private fun toRawValue(element: JsonElement): RawValue = when {
        element is JsonNull -> RawValue.Null
        // isString is false for numbers and booleans. toLongOrNull() then rejects
        // booleans, decimals like 1.5 and numbers too big for a Long.
        element is JsonPrimitive && !element.isString ->
            element.content.toLongOrNull()?.let { RawValue.Integer(it) } ?: RawValue.Invalid(element.content)
        else -> RawValue.Invalid(element.toString())
    }
}
