package com.navvistest.mobilechallenge.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChunkDecoderTest {

    @Test
    fun `decodes the numbers example from the challenge`() {
        val result = ChunkDecoder.decode("""{"numbers": [4, 150, 12, 21, 136, 16]}""", "numbers")

        val expected = listOf(4L, 150L, 12L, 21L, 136L, 16L).map { RawValue.Integer(it) }
        assertEquals(ApiResult.Success(expected), result)
    }

    @Test
    fun `keeps valid values when some elements are bad`() {
        val body = """{"computation_input": [10, null, "25", 1.5, true, {"a": 1}, 4123]}"""

        val result = ChunkDecoder.decode(body, "computation_input")

        val expected = listOf(
            RawValue.Integer(10),
            RawValue.Null,
            RawValue.Invalid("\"25\""),
            RawValue.Invalid("1.5"),
            RawValue.Invalid("true"),
            RawValue.Invalid("""{"a":1}"""),
            RawValue.Integer(4123),
        )
        assertEquals(ApiResult.Success(expected), result)
    }

    @Test
    fun `fails when the expected key is missing`() {
        val result = ChunkDecoder.decode("""{"values": [1, 2]}""", "numbers")

        assertEquals(ApiResult.Failure(ApiError.MalformedBody("Missing key \"numbers\"")), result)
    }

    @Test
    fun `fails when the body is not JSON`() {
        val result = ChunkDecoder.decode("<html>Not found</html>", "numbers")

        assertEquals(ApiResult.Failure(ApiError.MalformedBody("Body is not valid JSON")), result)
    }
}
