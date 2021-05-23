package com.lb.demo

import akka.actor.typed.ActorSystem

object LoadBalancerApp extends App {
  val system: ActorSystem[Server.Message] =
    ActorSystem(Server("localhost", 8080), "LoadBalancerServer")
}
