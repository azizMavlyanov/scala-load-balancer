package com.lb.demo

object ServerDomain {
  final case class ServerDetails(ip: String, maxThroughput: Int, availableThroughput: Int)
  final case class ServerErrorDetails(message: String)
}
