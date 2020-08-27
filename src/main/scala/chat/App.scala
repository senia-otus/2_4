package chat

import java.time.Instant
import java.util.UUID

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.typed.Cluster
import akka.management.scaladsl.AkkaManagement
import akka.util.Timeout
import chat.App.{StartCharListener, SystemCommand}
import chat.MessageSender.Env
import chat.model.Message
import zio._
import zio.clock.Clock
import zio.duration._
import zio.stream._

class MessageSender(writer: EntityRef[ChatWriter.Command])(implicit system: ActorSystem[Nothing]) {
  import MessageSender._

  def sendMessage(text: String): RIO[Env, Unit] = {
    implicit val timeout: Timeout = Timeout.create(5.minutes.asJava)

    def trySend(message: Message): RIO[Clock, Unit] =
      ZIO.fromFuture { _ =>
        writer.ask[ChatWriter.Reply](ChatWriter.ProcessMessage(message, _))
      } *> ZIO.unit

    trySend(Message(UUID.randomUUID(), text, Instant.now()))
//      .retry(Schedule.spaced(5.second).untilOutput(_ > 100))
  }

  def spamMessages(count: Int)(text: Int => String): RIO[Env, Unit] = {
    Stream
      .fromIterable(0 until count)
      .map(text)
      .foreach(sendMessage)
  }

  def sendMessages(count: Int, timeout: Duration)(text: Int => String): RIO[Env, Unit] = {
    Stream
      .fromIterable(0 until count)
      .map(text)
      .foreach(sendMessage(_) *> ZIO.sleep(timeout))
  }
}

object MessageSender {
  type Env = Clock
}

case class App(system: ActorSystem[SystemCommand], runtime: Runtime[Clock]) {
  private implicit val sys: ActorSystem[SystemCommand] = system

  private def makeSender(chatName: String): MessageSender = {
    new MessageSender(ClusterSharding(system).entityRefFor(ChatWriter.typeKey, chatName))
  }

  private def spamToChats(chats: Int, messages: Int)(text: Int => Int => String): RIO[Env, Unit] =
    ZIO.foreachPar_(0 until chats) { chatId =>
      makeSender(s"Chat $chatId").spamMessages(messages)(text(chatId))
    }

  private def sendToChats(chats: Int, messages: Int)(text: Int => Int => String): RIO[Env, Unit] =
    ZIO.foreachPar_(0 until chats) { chatId =>
      makeSender(s"Chat $chatId").sendMessages(messages, 100.milliseconds)(text(chatId))
    }

  def spamToAll(): Unit = {
    runtime.unsafeRun(spamToChats(1000, 10)(chat => msg => s"Init. Chat: $chat; Message: $msg"))
  }

  def sendSlow(): Unit = {
    runtime.unsafeRun(sendToChats(8, 1000)(chat => msg => s"Slow. Chat: $chat; Message: $msg"))
  }

  def startListeners(): Unit = {
    implicit val timeout: Timeout = Timeout.create(1.second.asJava)

    val res = ZIO.foreachPar_(0 until 8) { chatId =>
      ZIO.fromFuture(_ =>
        system.ask[ActorRef[ChatListener.Command]](StartCharListener(s"Chat $chatId", _))
      )
    }

    runtime.unsafeRun(res)
  }
}

object App {
  sealed trait SystemCommand
  case class StartCharListener(chatName: String, replyTo: ActorRef[ActorRef[ChatListener.Command]])
      extends SystemCommand

  def behavior: Behavior[SystemCommand] =
    Behaviors.setup { ctx =>
      Behaviors.receiveMessage {
        case StartCharListener(chatName, replyTo) =>
          replyTo ! ctx.spawnAnonymous(ChatListener(chatName))
          Behaviors.same
      }
    }

  def start(): App = {
    implicit val system: ActorSystem[SystemCommand] = ActorSystem(behavior, "chat")

    val cluster = Cluster(system)

    if (cluster.selfMember.roles("management")) {
      AkkaManagement(system).start()
    }

    val sharding = ClusterSharding(system)

    // TODO init reader
    // TODO init writer

    App(system, Runtime.default)
  }
}
