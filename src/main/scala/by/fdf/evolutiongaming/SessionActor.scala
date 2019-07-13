package by.fdf.evolutiongaming

import akka.actor.Actor
import by.fdf.evolutiongaming.SessionActor._
import by.fdf.evolutiongaming.WebProtocol._

/**
 * @author Dzmitry Fursevich
 */
object SessionActor {

  trait SessionMessage

  case class Session(id: String, name: String, password: String, role: String)

  case class LoginMessage(sessionId: String, username: String, password: String) extends SessionMessage

  case class IsAuthorizedMessage(sessionId: String, request: Request) extends SessionMessage

  case class IsAuthorizedResultMessage(authorized: Boolean) extends SessionMessage

}

class SessionActor extends Actor {
  var sessions: Map[String, Session] = Map.empty

  override def receive: Receive = {
    case LoginMessage(sessionId, username, password) =>
      val session = Session(sessionId, username, password, if (username == "admin") "admin" else "user")
      sessions += sessionId -> session
      sender() ! LoginSuccessful(session.role)
    case IsAuthorizedMessage(sessionId, request) =>
      if (sessions.contains(sessionId)) {
        val session = sessions(sessionId)
        request match {
          case _: AddTable | _: UpdateTable | _: RemoveTable =>
            sender() ! IsAuthorizedResultMessage(session.role == "admin")
          case _ =>
            sender() ! IsAuthorizedResultMessage(true)
        }
      } else {
        sender() ! IsAuthorizedResultMessage(false)
      }
  }
}
