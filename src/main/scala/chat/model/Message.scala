package chat.model

import java.time.Instant
import java.util.UUID

case class Message(idempotenceKey: UUID, text: String, sent: Instant)
