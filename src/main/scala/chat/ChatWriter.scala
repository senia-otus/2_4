package chat

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import chat.model.{ChatState, Message}
import chat.serialization.CborSerializable

object ChatWriter {
  sealed trait Command                                                  extends CborSerializable
  case class ProcessMessage(message: Message, replyTo: ActorRef[Reply]) extends Command

  sealed trait Reply    extends CborSerializable
  case object Success   extends Reply
  case object Duplicate extends Reply

  sealed trait Event                           extends CborSerializable
  case class MessageReceived(message: Message) extends Event

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ChatWriter")

  def apply(chatName: String): Behavior[Command] =
    Behaviors.setup { ctx =>
      // TODO:
      // val reader = ???

      // TODO
      val commandHandler: (ChatState, Command) => Effect[Event, ChatState] = ???

      // TODO
      val eventHandler: (ChatState, Event) => ChatState = (state, event) => ???

      // TODO
      // EventSourcedBehavior[Command, Event, ChatState]

      // TODO withRetention

      ???
    }
}
