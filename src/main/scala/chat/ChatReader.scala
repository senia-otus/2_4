package chat

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import chat.model.{ChatState, Message}
import chat.serialization.CborSerializable

object ChatReader {
  sealed trait Command extends CborSerializable
  case class Subscribe(subscriber: ActorRef[Notification]) extends Command
  case class UpdateState(state: ChatState) extends Command
  case class ProcessMessage(message: Message, state: ChatState) extends Command
  case object Stop extends Command
  case class Unsubscribe(subscriber: ActorRef[Notification]) extends Command

  sealed trait Notification extends CborSerializable
  case class LatestMessages(messages: Seq[Message]) extends Notification
  case class ChatMessage(message: Message) extends Notification

  def serviceKey(chatName: String): ServiceKey[Command] = ServiceKey[Command](chatName)

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ChatReader")

  def apply(chatName: String): Behavior[Command] = Behaviors.setup { ctx =>
    ctx.system.receptionist ! Receptionist.register(serviceKey(chatName), ctx.self)

    def inner(subscribers: Set[ActorRef[Notification]], state: ChatState): Behavior[Command] =
      Behaviors.receiveMessage {
        case Subscribe(subscriber) =>
          subscriber ! LatestMessages(state.messages.lastN)
          ctx.watchWith(subscriber, Unsubscribe(subscriber))
          inner(subscribers + subscriber, state)
        case Unsubscribe(subscriber) => inner(subscribers - subscriber, state)
        case UpdateState(state) =>
          inner(subscribers, state)
        case ProcessMessage(message, state) =>
          subscribers.foreach(_ ! ChatMessage(message))
          inner(subscribers, state)
        case Stop =>
          Behaviors.stopped
      }

    inner(Set.empty, ChatState.empty)
  }
}
