/*
 * Copyright (C) 2024 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString

class DeflaterTest {
  @Test
  fun happyPath() {
    val deflater = Deflater().apply {
      source = "God help us, we're in the hands of engineers.".encodeUtf8().toByteArray()
      sourcePos = 0
      sourceLimit = source.size
      sourceFinished = true

      target = ByteArray(256)
      targetPos = 0
      targetLimit = target.size
    }

    assertTrue(deflater.deflate())
    assertEquals(deflater.sourceLimit, deflater.sourcePos)
    val deflated = deflater.target.toByteString(0, deflater.targetPos)

    // Golden compressed output.
    assertEquals(
      "c89PUchIzSlQKC3WUShPVS9KVcjMUyjJSFXISMxLKVbIT1NIzUvPzEtNLSrWAwA=".decodeBase64(),
      deflated,
    )

    deflater.close()
  }

  @Test
  fun deflateInParts() {
    val deflater = Deflater().apply {
      target = ByteArray(256)
      targetPos = 0
      targetLimit = target.size
    }

    deflater.source = "God help us, we're in the hands".encodeUtf8().toByteArray()
    deflater.sourcePos = 0
    deflater.sourceLimit = deflater.source.size
    deflater.sourceFinished = false
    assertTrue(deflater.deflate())
    assertEquals(deflater.sourceLimit, deflater.sourcePos)

    deflater.source = " of engineers.".encodeUtf8().toByteArray()
    deflater.sourcePos = 0
    deflater.sourceLimit = deflater.source.size
    deflater.sourceFinished = true
    assertTrue(deflater.deflate())
    assertEquals(deflater.sourceLimit, deflater.sourcePos)

    val deflated = deflater.target.toByteString(0, deflater.targetPos)

    // Golden compressed output.
    assertEquals(
      "c89PUchIzSlQKC3WUShPVS9KVcjMUyjJSFXISMxLKVbIT1NIzUvPzEtNLSrWAwA=".decodeBase64(),
      deflated,
    )

    deflater.close()
  }

  @Test
  fun deflateInsufficientSpaceInTargetWithoutSourceFinished() {
    val targetBuffer = Buffer()

    val deflater = Deflater().apply {
      source = "God help us, we're in the hands of engineers.".encodeUtf8().toByteArray()
      sourcePos = 0
      sourceLimit = source.size
    }

    deflater.target = ByteArray(10)
    deflater.targetPos = 0
    deflater.targetLimit = deflater.target.size
    assertFalse(deflater.deflate(flush = true))
    assertEquals(deflater.targetLimit, deflater.targetPos)
    targetBuffer.write(deflater.target)

    deflater.target = ByteArray(256)
    deflater.targetPos = 0
    deflater.targetLimit = deflater.target.size
    assertTrue(deflater.deflate())
    assertEquals(deflater.sourcePos, deflater.sourceLimit)
    targetBuffer.write(deflater.target, 0, deflater.targetPos)

    deflater.sourceFinished = true
    assertTrue(deflater.deflate())

    // Golden compressed output.
    assertEquals(
      "cs9PUchIzSlQKC3WUShPVS9KVcjMUyjJSFXISMxLKVbIT1NIzUvPzEtNLSrWAw==".decodeBase64(),
      targetBuffer.readByteString(),
    )

    deflater.close()
  }

  @Test
  fun deflateInsufficientSpaceInTargetWithSourceFinished() {
    val targetBuffer = Buffer()

    val deflater = Deflater().apply {
      source = "God help us, we're in the hands of engineers.".encodeUtf8().toByteArray()
      sourcePos = 0
      sourceLimit = source.size
      sourceFinished = true
    }

    deflater.target = ByteArray(10)
    deflater.targetPos = 0
    deflater.targetLimit = deflater.target.size
    assertFalse(deflater.deflate())
    assertEquals(deflater.targetLimit, deflater.targetPos)
    targetBuffer.write(deflater.target)

    deflater.target = ByteArray(256)
    deflater.targetPos = 0
    deflater.targetLimit = deflater.target.size
    assertTrue(deflater.deflate())
    assertEquals(deflater.sourcePos, deflater.sourceLimit)
    targetBuffer.write(deflater.target, 0, deflater.targetPos)

    // Golden compressed output.
    assertEquals(
      "c89PUchIzSlQKC3WUShPVS9KVcjMUyjJSFXISMxLKVbIT1NIzUvPzEtNLSrWAwA=".decodeBase64(),
      targetBuffer.readByteString(),
    )

    deflater.close()
  }

  @Test
  fun deflateEmptySource() {
    val deflater = Deflater().apply {
      sourceFinished = true

      target = ByteArray(256)
      targetPos = 0
      targetLimit = target.size
    }

    assertTrue(deflater.deflate())
    val deflated = deflater.target.toByteString(0, deflater.targetPos)

    // Golden compressed output.
    assertEquals(
      "AwA=".decodeBase64(),
      deflated,
    )

    deflater.close()
  }

  @Test
  fun cannotDeflateAfterClose() {
    val deflater = Deflater()
    deflater.close()

    assertFailsWith<IllegalStateException> {
      deflater.deflate()
    }
  }

  @Test
  fun closeIsIdemptent() {
    val deflater = Deflater()
    deflater.close()
    deflater.close()
  }
}
