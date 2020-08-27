package chat

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import chat.model.{ChatState, Message}
import chat.serialization.CborSerializable

object ChatWriter {
  sealed trait Command extends CborSerializable
  case class ProcessMessage(message: Message, replyTo: ActorRef[Reply]) extends Command

  sealed trait Reply extends CborSerializable
  case object Success extends Reply
  case object Duplicate extends Reply

  sealed trait Event extends CborSerializable
  case class MessageReceived(message: Message) extends Event

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ChatWriter")

  def apply(chatName: String): Behavior[Command] = Behaviors.setup { ctx =>
    val reader = ClusterSharding(ctx.system).entityRefFor(ChatReader.typeKey, chatName)


    val commandHandler: (ChatState, Command) => Effect[Event, ChatState] = (state, command) => {
      command match {
        case ProcessMessage(msg, replyTo) if state.ids.contains(msg.idempotenceKey) =>
          Effect.none.thenReply(replyTo)(_ => Duplicate)

        case ProcessMessage(message, replyTo) =>
          Effect.persist(MessageReceived(message)).thenRun { newState =>
            reader ! ChatReader.ProcessMessage(message, newState)
            replyTo ! Success
          }
      }
    }

    val eventHandler: (ChatState, Event) => ChatState = (state, event) => event match {
      case MessageReceived(message) =>
        val id = message.idempotenceKey
        if (state.ids.contains(id)) state.copy(ids = state.ids.add(id))
        else state.copy(
          messages = state.messages.add(message),
          ids = state.ids.add(id)
        )
    }
    EventSourcedBehavior[Command, Event, ChatState](
      persistenceId = PersistenceId("ChatWriter", chatName),
      emptyState = ChatState.empty,
      commandHandler = commandHandler,
      eventHandler = eventHandler
    ).withRetention(RetentionCriteria.snapshotEvery(20, 2))
  }
}
