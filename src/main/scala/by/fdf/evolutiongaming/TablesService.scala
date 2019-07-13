package by.fdf.evolutiongaming

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Merge, Partition, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import akka.util.Timeout
import by.fdf.evolutiongaming.SessionActor.{IsAuthorizedMessage, IsAuthorizedResultMessage, LoginMessage}
import by.fdf.evolutiongaming.TablesActor._
import by.fdf.evolutiongaming.WebProtocol.JsonSupport._
import by.fdf.evolutiongaming.WebProtocol._
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * @author Dzmitry Fursevich
 */
object TablesService {
  def apply()(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) = new TablesService()
}

class TablesService(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {

  private[this] val tablesActor = actorSystem.actorOf(Props(classOf[TablesActor]))
  private[this] val sessionActor = actorSystem.actorOf(Props(classOf[SessionActor]))

  def websocketFlow(sessionId: String): Flow[Message, Message, _] = {
    val (responseActor, publisher) = Source
      .actorRef[Response](65535, OverflowStrategy.fail)
      .toMat(Sink.asPublisher(fanout = false))(Keep.both)
      .run()

    Flow.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._
        import actorSystem.dispatcher

        val fromWebsocket = builder.add(Flow[Message]
          .mapAsync(1) {
            case tm: TextMessage => tm.textStream.runFold("")(_ + _).map(Some(_))
            case bm: BinaryMessage =>
              bm.dataStream.runWith(Sink.ignore)
              Future.successful(None)
          }.collect {
          case Some(txt) => txt.parseJson.convertTo[Request]
        })

        val incomingPartition = builder.add(Partition[Request](3, {
          case _: Ping => 0
          case _: Login => 1
          case _ => 2
        }))

        val pingMessages = builder.add(Flow[Request]
          .collect {
            case Ping(seq) => Pong(seq)
          })

        implicit val askTimeout = Timeout(30.seconds)
        val loginMessages = builder.add(Flow[Request]
          .collect {
            case Login(username, password) => LoginMessage(sessionId, username, password)
          }.ask[Response](sessionActor))

        val authorizationMessages = builder.add(Flow[Request]
          .mapAsync(1)(req => {
            import akka.pattern.ask
            (sessionActor ? IsAuthorizedMessage(sessionId, req))
              .mapTo[IsAuthorizedResultMessage]
              .map(ar => (req, ar.authorized))
          }))

        val authorizationResultPartition = builder.add(Partition[(Request, Boolean)](2, {
          case (_, true) => 0
          case (_, false) => 1
        }))

        val tableMessages = builder.add(Flow[(Request, Boolean)]
          .map(_._1)
          .collect {
            case AddTable(afterId, name, participants) => AddTableMessage(afterId, name, participants)
            case UpdateTable(id, name, participants) => UpdateTableMessage(id, name, participants)
            case RemoveTable(id) => RemoveTableMessage(id)
            case SubscribeTables() => SubscribeTablesMessage(sessionId, responseActor)
            case UnsubscribeTables() => UnsubscribeTablesMessage(sessionId)
          }
          .ask[Response](tablesActor))

        val notAuthorizedMessages = builder.add(Flow[(Request, Boolean)].map(_ => NotAuthorized()))

        val backToWebsocket = builder.add(Flow[Response].map(resp => TextMessage(resp.toJson.prettyPrint)))

        val responseSource = builder.add(Source.fromPublisher(publisher))

        val merge = builder.add(Merge[Response](5))

        fromWebsocket ~> incomingPartition
        incomingPartition.out(0) ~> pingMessages ~> merge.in(0)
        incomingPartition.out(1) ~> loginMessages ~> merge.in(1)
        incomingPartition.out(2) ~> authorizationMessages ~> authorizationResultPartition

        authorizationResultPartition.out(0) ~> tableMessages ~> merge.in(2)
        authorizationResultPartition.out(1) ~> notAuthorizedMessages ~> merge.in(3)
        responseSource ~> merge.in(4)

        merge.out ~> backToWebsocket

        FlowShape.of(fromWebsocket.in, backToWebsocket.out)
    })
  }
}
