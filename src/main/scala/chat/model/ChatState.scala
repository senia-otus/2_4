package chat.model

import java.util.UUID

import chat.serialization.CborSerializable

case class ChatState(messages: LastNSeq[Message], ids: LastNSet[UUID]) extends CborSerializable

object ChatState {
  def empty: ChatState = ChatState(LastNSeq.empty(100), LastNSet.empty(1000))
}
