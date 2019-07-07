package by.fdf.evolutiongaming

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import by.fdf.evolutiongaming.TablesActor._
import by.fdf.evolutiongaming.WebProtocol.JsonSupport._
import by.fdf.evolutiongaming.WebProtocol._
import spray.json._

import scala.concurrent.Future

/**
 * @author Dzmitry Fursevich
 */
object TablesService {
  def apply()(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) = new TablesService()
}

class TablesService(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {

  private[this] val tablesActor = actorSystem.actorOf(Props(classOf[TablesActor]))

  def websocketFlow(user: String): Flow[Message, Message, _] = {
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
        }.map {
          case AddTable(afterId, name, participants) => AddTableMessage(afterId, name, participants, responseActor)
          case UpdateTable(id, name, participants) => UpdateTableMessage(id, name, participants, responseActor)
          case RemoveTable(id) => RemoveTableMessage(id, responseActor)
          case SubscribeTables() => SubscribeTablesMessage(user, responseActor)
          case UnsubscribeTables() => UnsubscribeTablesMessage(user)
        })

        val backToWebsocket = builder.add(Flow[Response]
          .map(resp => TextMessage(resp.toJson.prettyPrint)))

        val tablesActorSink = builder.add(Sink.actorRef[TableMessage](tablesActor, UnsubscribeTablesMessage(user)))

        val responseSource = builder.add(Source.fromPublisher(publisher))

        //        val merge = builder.add(Merge[TableMessage](2))
        //        val actorConnected = Source.single(SubscribeTablesMessage(user, responseActor))
        //
        //        fromWebsocket ~> merge.in(0)
        //        actorConnected ~> merge.in(1)
        //        merge ~> tablesActorSink

        fromWebsocket ~> tablesActorSink

        responseSource ~> backToWebsocket

        FlowShape.of(fromWebsocket.in, backToWebsocket.out)
    })
  }
}
