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

  case class SubscribeTablesMessage(sessionId: String, actor: ActorRef) extends TableMessage

  case class UnsubscribeTablesMessage(sessionId: String) extends TableMessage

  case class AddTableMessage(afterId: Int, name: String, participants: Int) extends TableMessage

  case class UpdateTableMessage(id: Int, name: String, participants: Int) extends TableMessage

  case class RemoveTableMessage(id: Int) extends TableMessage

}

class TablesActor extends Actor {

  var tables: ListBuffer[Table] = ListBuffer.empty
  var subscribers: Map[String, ActorRef] = Map.empty
  var ids = (1 to Int.MaxValue).view.iterator

  override def receive: Receive = {
    case AddTableMessage(afterId, name, participants) =>
      if (afterId != -1) {
        val idx = tables.indexWhere(_.id == afterId)
        if (idx == -1) {
          sender() ! AddFailed(afterId)
        } else {
          addTable(name, participants, idx + 1, afterId)
        }
      } else {
        addTable(name, participants, 0, afterId)
      }
    case UpdateTableMessage(id, name, participants) =>
      val idx = tables.indexWhere(_.id == id)
      if (idx == -1) {
        sender() ! UpdateFailed(id)
      } else {
        val table = Table(id, name, participants)
        tables.update(idx, table)
        sender() ! UpdateSuccessful(table.id)
        broadcast(TableUpdated(table))
      }
    case RemoveTableMessage(id) =>
      val idx = tables.indexWhere(_.id == id)
      if (idx == -1) {
        sender() ! RemovalFailed(id)
      } else {
        tables.remove(idx)
        sender() ! RemovalSuccessful(id)
        broadcast(TableRemoved(id))
      }
    case SubscribeTablesMessage(user, actor) =>
      subscribers += user -> actor
      sender() ! TableList(tables.toList)
    case UnsubscribeTablesMessage(user) =>
      subscribers -= user
      sender() ! UnsubscribeTablesSuccessful()
  }

  def addTable(name: String, participants: Int, idx: Int, afterId: Int): Unit = {
    val table = Table(ids.next(), name, participants)
    tables.insert(idx, table)
    sender() ! AddSuccessful(table.id)
    broadcast(TableAdded(afterId, table))
  }

  def broadcast(message: Response): Unit = subscribers.values.foreach(_ ! message)
}
