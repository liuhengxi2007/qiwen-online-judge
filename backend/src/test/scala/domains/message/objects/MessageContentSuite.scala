package domains.message.objects

import munit.FunSuite

class MessageContentSuite extends FunSuite:

  test("parse rejects empty content") {
    assertEquals(MessageContent.parse("   "), Left("Message content is required."))
  }

  test("parse rejects overlong content") {
    assertEquals(
      MessageContent.parse("x" * 5001),
      Left("Message content must be at most 5000 characters.")
    )
  }

  test("parse trims and accepts valid content") {
    assertEquals(MessageContent.parse("  hello  "), Right(MessageContent("hello")))
  }
