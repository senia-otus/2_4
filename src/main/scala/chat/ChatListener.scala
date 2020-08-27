package chat

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import chat.model.Message
import chat.serialization.CborSerializable

object ChatListener {
  private sealed trait PrivateCommand extends Command
  private case class ProcessLatestMessages(messages: Seq[Message]) extends PrivateCommand
  private case class ProcessMessage(message: Message) extends PrivateCommand
  private case class Listing(listing: Receptionist.Listing) extends PrivateCommand

  sealed trait Command
  case object Stop extends Command with CborSerializable

  def apply(chatName: String): Behavior[Command] = Behaviors.setup[Command] { ctx =>
    val listingAdapter = ctx.messageAdapter[Receptionist.Listing](Listing)
    val serviceKey = ChatReader.serviceKey(chatName)
    ctx.system.receptionist ! Receptionist.Subscribe(serviceKey, listingAdapter)

    val notificationAdapter = ctx.messageAdapter[ChatReader.Notification] {
      case ChatReader.LatestMessages(messages) => ProcessLatestMessages(messages)
      case ChatReader.ChatMessage(message) => ProcessMessage(message)
    }

    val entity = ClusterSharding(ctx.system).entityRefFor(ChatReader.typeKey, chatName)
    entity ! ChatReader.Subscribe(notificationAdapter)

    Behaviors.receiveMessage {
      case ProcessLatestMessages(messages) =>
        ctx.log.info(s"Received message list of size ${messages.size}")
        Behaviors.same
      case ProcessMessage(message) =>
        ctx.log.info(s"Received message $message")
        Behaviors.same
      case Listing(serviceKey.Listing(listing)) =>
        listing.foreach(_ ! ChatReader.Subscribe(notificationAdapter))
        Behaviors.same
      case Stop => Behaviors.stopped
    }
  }.narrow
}
