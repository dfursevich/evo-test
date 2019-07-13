package by.fdf.evolutiongaming

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.io.StdIn

/**
 * @author Dzmitry Fursevich
 */
object TablesServer extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  var tablesService = TablesService()

  val route =
    path("") {
      get {
        handleWebSocketMessages(tablesService.websocketFlow(UUID.randomUUID().toString))
      }
    }

  val bindingFuture =
    Http().bindAndHandle(route, interface = "localhost", port = 8087)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()

  import system.dispatcher

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
