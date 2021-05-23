package com.lb.demo

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.actor.typed.DispatcherSelector
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.lb.demo.LoadBalancerRepository.{ReleaseServer, ReserveServer}
import com.lb.demo.ServerDomain.{ServerDetails, ServerErrorDetails}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Server extends JsonSupport {
  implicit val timeout: Timeout = 3.seconds

  sealed trait Message

  final case class AcceptServerReservation(server: ServerDetails) extends Message

  case object AcceptServerRelease extends Message

  final case class DeclineServerReservation(reason: String) extends Message

  case object DeclineServerRelease extends Message

  private final case class StartFailed(cause: Throwable) extends Message

  private final case class Started(binding: ServerBinding) extends Message

  case object Stop extends Message

  def apply(host: String, port: Int): Behavior[Message] = Behaviors.setup { context =>
    implicit val system: ActorSystem[Nothing] = context.system
    val loadBalancer = context.spawn(LoadBalancerRepository(), "LoadBalancerRepository")
    implicit val executionContext: ExecutionContextExecutor = system.dispatchers
      .lookup(DispatcherSelector.fromConfig("route-dispatcher"))

    val route: Route =
      get {
        path("balance") {
          parameter(Symbol("throughput").as[Int]) { (throughput: Int) =>
            extractLog { (log: LoggingAdapter) =>
              log.info(s"Requesting server with throughput: $throughput ...")
              val response = loadBalancer.ask(ReserveServer(throughput, _))
              onSuccess(response) {
                case AcceptServerReservation(server) => complete(server)
                case DeclineServerReservation(reason) =>
                  complete(StatusCodes.InternalServerError -> ServerErrorDetails(reason))
                case _ => complete(StatusCodes.InternalServerError -> ServerErrorDetails("Error internally occured"))
              }
            }
          }
        }
      } ~
        post {
          path("end") {
            parameter(Symbol("server").as[String]) { (server: String) =>
              extractLog { (log: LoggingAdapter) =>
                log.info(s"Querying to release server with IP: $server ...")
                val response = loadBalancer.ask(ReleaseServer(server, _))
                onSuccess(response) {
                  case AcceptServerRelease => complete(StatusCodes.OK)
                  case DeclineServerRelease => complete(StatusCodes.Forbidden)
                  case _ => complete(StatusCodes.InternalServerError -> ServerErrorDetails("Error internally occurred"))
                }
              }
            }
          }
        }


    val serverBindingFuture = Http().newServerAt(host, port).bind(route)

    context.pipeToSelf(serverBindingFuture) {
      case Success(binding) => Started(binding)
      case Failure(ex) => StartFailed(ex)
    }

    def running(binding: ServerBinding): Behavior[Message] =
      Behaviors.receiveMessagePartial[Message] {
        case Stop =>
          context.log.info(
            "Stopping server http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          Behaviors.stopped
      }.receiveSignal {
        case (_, PostStop) =>
          binding.unbind()
          Behaviors.same
      }

    def starting(wasStopped: Boolean): Behaviors.Receive[Message] =
      Behaviors.receiveMessage[Message] {
        case StartFailed(cause) =>
          throw new RuntimeException("Server failed to start", cause)
        case Started(binding) =>
          context.log.info(
            "Server online at http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          if (wasStopped) context.self ! Stop
          running(binding)
        case Stop =>
          starting(wasStopped = true)
      }

    starting(wasStopped = false)
  }

}
