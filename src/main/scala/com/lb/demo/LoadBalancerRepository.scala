package com.lb.demo

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.lb.demo.Server.{AcceptServerRelease, AcceptServerReservation, DeclineServerRelease, DeclineServerReservation}
import pureconfig._
import pureconfig.generic.auto._

import scala.annotation.tailrec

object LoadBalancerRepository {
  val PATH = "src/main/resources/servers.conf"

  import ServerDomain._

  sealed trait Command

  final case class ReserveServer(throughput: Int, replyTo: ActorRef[Server.Message]) extends Command

  final case class ReleaseServer(server: String, replyTo: ActorRef[Server.Message]) extends Command

  final case class ServersConf(servers: List[ServerDetails])

  def apply(): Behavior[Command] = balanceLoad(loadServersConfig(PATH))

  private def balanceLoad(servers: List[ServerDetails]): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case ReserveServer(throughput, replyTo) =>
          context.log.info(s"Balancing servers with throughput: $throughput")
          reserveServer(servers, throughput) match {
            case Some(value) =>
              replyTo ! AcceptServerReservation(value._1)
              balanceLoad(value._2)
            case _ =>
              replyTo ! DeclineServerReservation("No free server available")
              Behaviors.same
          }
        case ReleaseServer(server, replyTo) =>
          context.log.info(s"Releasing server with ip: $server")
          releaseServer(server, servers) match {
            case Some(value) =>
              replyTo ! AcceptServerRelease
              balanceLoad(value)
            case _ =>
              replyTo ! DeclineServerRelease
              Behaviors.same
          }
        case _ => Behaviors.same
      }
    }

  private def loadServersConfig(path: String): List[ServerDetails] = ConfigSource.file(path).load[ServersConf] match {
    case Right(ServersConf(servers)) => servers
    case Left(_) => List[ServerDomain.ServerDetails]()
  }

  private def releaseServer(ip: String, servers: List[ServerDomain.ServerDetails]): Option[List[ServerDetails]] = {
    val releasedServer = servers
      .filter((server) => server.ip == ip)
      .map((filteredServer) => {
        filteredServer.copy(availableThroughput = filteredServer.maxThroughput)
      })

    Some(servers.filter((server) => server.ip != releasedServer.head.ip) ++ releasedServer)
  }

  private def reserveServer(servers: List[ServerDomain.ServerDetails], throughput: Int): Option[(ServerDetails, List[ServerDetails])] = {
    val server = getServer(servers, throughput)
    server match {
      case Some(serverDetails) =>
        val modifiedServer = serverDetails.copy(availableThroughput = serverDetails.availableThroughput - throughput)
        val serverList = servers.filter((el) => el.ip != serverDetails.ip) ++ List(modifiedServer)
        Some((modifiedServer, serverList))
      case _ => None
    }
  }

  private def getServer(servers: List[ServerDomain.ServerDetails], throughput: Int): Option[ServerDomain.ServerDetails] = {
    if (servers.isEmpty) {
      None
    } else {
      val server = findTheLeastLoadedServer(servers)
      if (throughput <= server.availableThroughput)
        Some(server)
      else
        None
    }
  }

  private def findTheLeastLoadedServer(servers: List[ServerDomain.ServerDetails]): ServerDomain.ServerDetails = {
    @tailrec
    def findTheLeastLoadedServerTailrec(remaining: List[ServerDomain.ServerDetails],
                                        currentServer: ServerDomain.ServerDetails): ServerDomain.ServerDetails = {
      if (remaining.isEmpty) currentServer
      else if (remaining.head.availableThroughput >= currentServer.availableThroughput)
        findTheLeastLoadedServerTailrec(remaining.drop(1), remaining.head)
      else
        findTheLeastLoadedServerTailrec(remaining.drop(1), currentServer)

    }

    findTheLeastLoadedServerTailrec(servers.drop(1), servers.head)
  }
}
