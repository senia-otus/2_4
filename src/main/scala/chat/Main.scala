package chat

import akka.cluster.typed.Cluster

import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    val app = App.start()

    if (Cluster(app.system).selfMember.roles("sender")) {
      println("Press Enter to start 8 listeners")
      StdIn.readLine()
      app.startListeners()

      println("Press Enter to send messages to 1000 chats")
      StdIn.readLine()
      app.spamToAll()

      println("Press Enter to slowly send 1000 messages to 8 chats")
      StdIn.readLine()
      app.sendSlow()
    }

    println("Press Enter to exit")
    StdIn.readLine()

    app.system.terminate()
  }
}
