package chat

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import chat.model.Message
import chat.serialization.CborSerializable

object ChatListener {
  private sealed trait PrivateCommand                              extends Command
  private case class ProcessLatestMessages(messages: Seq[Message]) extends PrivateCommand
  private case class ProcessMessage(message: Message)              extends PrivateCommand
  private case class Listing(listing: Receptionist.Listing)        extends PrivateCommand

  sealed trait Command
  case object Stop extends Command with CborSerializable

  def apply(chatName: String): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      // TODO (2) subscribe to ChatReader update

      // TODO subscribe to ChatReader

      Behaviors.receiveMessage {
        case ProcessLatestMessages(messages) =>
          ctx.log.info(s"Received message list of size ${messages.size}")
          Behaviors.same
        case ProcessMessage(message) =>
          ctx.log.info(s"Received message $message")
          Behaviors.same
        // TODO update
        case Stop => Behaviors.stopped
      }
    }
}
