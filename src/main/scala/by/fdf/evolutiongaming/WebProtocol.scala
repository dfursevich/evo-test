package by.fdf.evolutiongaming

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.scalapenos.spray.SnakifiedSprayJsonSupport
import spray.json.{JsObject, JsString, JsValue, RootJsonFormat, _}

/**
 * @author Dzmitry Fursevich
 */
object WebProtocol {

  case class Table(id: Int, name: String, participants: Int)

  trait Request

  trait Response

  case class Login(username: String, password: String) extends Request

  case class LoginSuccessful(userType: String) extends Response

  case class LoginFailed() extends Response

  case class Ping(seq: Int) extends Request

  case class Pong(seq: Int) extends Response

  case class SubscribeTables() extends Request

  case class TableList(tables: Iterable[Table]) extends Response

  case class UnsubscribeTables() extends Request

  case class UnsubscribeTablesSuccessful() extends Response

  case class NotAuthorized() extends Response

  case class AddTable(afterId: Int, name: String, participants: Int) extends Request

  case class UpdateTable(id: Int, name: String, participants: Int) extends Request

  case class RemoveTable(id: Int) extends Request

  case class AddSuccessful(id: Int) extends Response

  case class RemovalSuccessful(id: Int) extends Response

  case class UpdateSuccessful(id: Int) extends Response

  case class AddFailed(afterId: Int) extends Response

  case class RemovalFailed(id: Int) extends Response

  case class UpdateFailed(id: Int) extends Response

  case class TableAdded(afterId: Int, table: Table) extends Response

  case class TableUpdated(table: Table) extends Response

  case class TableRemoved(id: Int) extends Response

  object JsonSupport extends SprayJsonSupport with SnakifiedSprayJsonSupport {
    implicit val tableFormat = jsonFormat3(Table)
    implicit val loginFormat = jsonFormat2(Login)
    implicit val loginSuccessfulFormat = jsonFormat1(LoginSuccessful)
    implicit val loginFailedFormat = jsonFormat0(LoginFailed)
    implicit val pingFormat = jsonFormat1(Ping)
    implicit val pongFormat = jsonFormat1(Pong)
    implicit val subscribeTablesFormat = jsonFormat0(SubscribeTables)
    implicit val tableListFormat = jsonFormat1(TableList)
    implicit val unsubscribeTablesFormat = jsonFormat0(UnsubscribeTables)
    implicit val notAuthorizedFormat = jsonFormat0(NotAuthorized)
    implicit val addTableFormat = jsonFormat3(AddTable)
    implicit val updateTableFormat = jsonFormat3(UpdateTable)
    implicit val removeTableFormat = jsonFormat1(RemoveTable)
    implicit val addFailedFormat = jsonFormat1(AddFailed)
    implicit val removalFailedFormat = jsonFormat1(RemovalFailed)
    implicit val updateFailedFormat = jsonFormat1(UpdateFailed)
    implicit val tableAddedFormat = jsonFormat2(TableAdded)
    implicit val tableUpdatedFormat = jsonFormat1(TableUpdated)
    implicit val tableRemovedFormat = jsonFormat1(TableRemoved)
    implicit val unsubscribeTablesSuccessfulFormat = jsonFormat0(UnsubscribeTablesSuccessful)
    implicit val addSuccessfulFormat = jsonFormat1(AddSuccessful)
    implicit val removalSuccessfulFormat = jsonFormat1(RemovalSuccessful)
    implicit val updateSuccessfulFormat = jsonFormat1(UpdateSuccessful)

    implicit val responseFormat = new RootJsonFormat[Response] {
      override def write(obj: Response): JsValue = JsObject((obj match {
        case ls: LoginSuccessful => ls.toJson
        case lf: LoginFailed => lf.toJson
        case p: Pong => p.toJson
        case tl: TableList => tl.toJson
        case na: NotAuthorized => na.toJson
        case af: AddFailed => af.toJson
        case rf: RemovalFailed => rf.toJson
        case uf: UpdateFailed => uf.toJson
        case ta: TableAdded => ta.toJson
        case tu: TableUpdated => tu.toJson
        case tr: TableRemoved => tr.toJson
        case uts: UnsubscribeTablesSuccessful => uts.toJson
        case as: AddSuccessful => as.toJson
        case us: UpdateSuccessful => us.toJson
        case rs: RemovalSuccessful => rs.toJson
      }).asJsObject.fields + ("$type" -> JsString(snakify(obj.getClass.getSimpleName))))

      override def read(json: JsValue): Response = null
    }

    implicit val requestFormat = new RootJsonFormat[Request] {
      def write(obj: Request): JsValue = JsObject()

      def read(json: JsValue): Request = json.asJsObject.getFields("$type") match {
        case Seq(JsString("login")) => json.convertTo[Login]
        case Seq(JsString("ping")) => json.convertTo[Ping]
        case Seq(JsString("subscribe_tables")) => json.convertTo[SubscribeTables]
        case Seq(JsString("unsubscribe_tables")) => json.convertTo[UnsubscribeTables]
        case Seq(JsString("add_table")) => json.convertTo[AddTable]
        case Seq(JsString("update_table")) => json.convertTo[UpdateTable]
        case Seq(JsString("remove_table")) => json.convertTo[RemoveTable]
      }
    }
  }

}
