package by.fdf.evolutiongaming

import akka.actor.{Actor, ActorRef}
import by.fdf.evolutiongaming.TablesActor._
import by.fdf.evolutiongaming.WebProtocol._

import scala.collection.mutable.ListBuffer

/**
 * @author Dzmitry Fursevich
 */
object TablesActor {

  trait TableMessage

  case class SubscribeTablesMessage(user: String, actor: ActorRef) extends TableMessage

  case class UnsubscribeTablesMessage(user: String) extends TableMessage

  case class AddTableMessage(afterId: Int, name: String, participants: Int, actor: ActorRef) extends TableMessage

  case class UpdateTableMessage(id: Int, name: String, participants: Int, actor: ActorRef) extends TableMessage

  case class RemoveTableMessage(id: Int, actor: ActorRef) extends TableMessage

}

class TablesActor extends Actor {

  var tables: ListBuffer[Table] = ListBuffer.empty
  var subscribers: Map[String, ActorRef] = Map.empty
  var ids = (1 to Int.MaxValue).view.iterator

  override def receive: Receive = {
    case AddTableMessage(afterId, name, participants, actor) =>
      if (afterId != -1) {
        val idx = tables.indexWhere(_.id == afterId)
        if (idx == -1) {
          actor ! AddFailed(afterId)
        } else {
          val table = Table(ids.next(), name, participants)
          tables.insert(idx + 1, table)
          broadcast(TableAdded(afterId, table))
        }
      } else {
        val table = Table(ids.next(), name, participants)
        tables.insert(0, table)
        broadcast(TableAdded(afterId, table))
      }
    case UpdateTableMessage(id, name, participants, actor) =>
      val idx = tables.indexWhere(_.id == id)
      if (idx == -1) {
        actor ! UpdateFailed(id)
      } else {
        val table = Table(id, name, participants)
        tables.update(idx, table)
        broadcast(TableUpdated(table))
      }
    case RemoveTableMessage(id, actor) =>
      val idx = tables.indexWhere(_.id == id)
      if (idx == -1) {
        sender() ! RemovalFailed(id)
      } else {
        tables.remove(idx)
        broadcast(TableRemoved(id))
      }
    case SubscribeTablesMessage(user, actor) =>
      subscribers += user -> actor
      actor ! TableList(tables.toList)
    case UnsubscribeTablesMessage(user) =>
      subscribers -= user
  }

  def broadcast(message: Response): Unit = subscribers.values.foreach(_ ! message)
}
