package example

import spray.routing.HttpService
import akka.actor.{ Props, Actor, ActorRef }
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.httpx.SprayJsonSupport._
import spray.http.StatusCodes

object ServiceActor {
  def props(model: ActorRef): Props = Props(new ServiceActor(model))
  def name = "service"
}

class ServiceActor(model: ActorRef) extends Actor with Service {
  def actorRefFactory = context
  def timeout = Timeout(1.second)
  def receive = runRoute(route(model))
}

trait Service extends HttpService with ModelJsonProtocol {
  implicit def ec = actorRefFactory.dispatcher
  implicit def timeout: Timeout

  def route(model: ActorRef) =
    get {
      path("items") {
        complete {
          (model ? 'list).mapTo[Seq[ItemSummary]]
        }
      } ~
        path("items" / IntNumber) { id =>
          onSuccess(model ? id) {
            case item: Item => complete(item)
            case None => complete(StatusCodes.NotFound, "Not Found")
          }
        }
    }

}