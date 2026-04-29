package domains.message.table

import domains.auth.model.Username
import munit.FunSuite

class MessageTableSupportSuite extends FunSuite:

  test("normalizeConversationPair orders usernames consistently") {
    val alice = Username.canonical("alice")
    val bob = Username.canonical("bob")

    assertEquals(MessageTableSupport.normalizeConversationPair(bob, alice), (alice, bob))
  }

  test("normalizeConversationPair keeps already ordered input unchanged") {
    val alice = Username.canonical("alice")
    val bob = Username.canonical("bob")

    assertEquals(MessageTableSupport.normalizeConversationPair(alice, bob), (alice, bob))
  }
