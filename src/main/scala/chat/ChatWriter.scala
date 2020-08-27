package chat

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import chat.ChatReader.UpdateState
import chat.model.{ChatState, Message}
import chat.serialization.CborSerializable

object ChatWriter {
  sealed trait Command                                                  extends CborSerializable
  case class ProcessMessage(message: Message, replyTo: ActorRef[Reply]) extends Command
  case class GetState(replyTo: ActorRef[ChatReader.UpdateState])        extends Command

  sealed trait Reply    extends CborSerializable
  case object Success   extends Reply
  case object Duplicate extends Reply

  sealed trait Event                           extends CborSerializable
  case class MessageReceived(message: Message) extends Event

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ChatWriter")

  def apply(chatName: String): Behavior[Command] =
    Behaviors.setup { ctx =>
      val reader = ClusterSharding(ctx.system).entityRefFor(ChatReader.typeKey, chatName)

      val commandHandler: (ChatState, Command) => Effect[Event, ChatState] = { (state, command) =>
        command match {
          case ProcessMessage(message, replyTo) if state.ids.contains(message.idempotenceKey) =>
            replyTo ! Duplicate
            Effect.none
          case ProcessMessage(message, replyTo) =>
            Effect.persist(MessageReceived(message)).thenRun { newState =>
              replyTo ! Success
              reader ! ChatReader.ProcessMessage(message, newState)
            }
          case GetState(replyTo) =>
            replyTo ! UpdateState(state)
            Effect.none
        }
      }

      val eventHandler: (ChatState, Event) => ChatState = (state, event) =>
        event match {
          case MessageReceived(message) =>
            val key = message.idempotenceKey
            if (state.ids.contains(key)) state.copy(ids = state.ids.add(key))
            else ChatState(messages = state.messages.add(message), ids = state.ids.add(key))
        }

      EventSourcedBehavior[Command, Event, ChatState](
        persistenceId = PersistenceId("ChatWriter", chatName),
        emptyState = ChatState.empty,
        commandHandler = commandHandler,
        eventHandler = eventHandler
      ).withRetention(RetentionCriteria.snapshotEvery(20, 2))
    }
}
