package com.lb.demo

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.lb.demo.ServerDomain.{ServerDetails, ServerErrorDetails}
import spray.json._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val serverDetailsFormat: RootJsonFormat[ServerDetails] = jsonFormat3(ServerDetails)
  implicit val serverErrorDetailsFormat: RootJsonFormat[ServerErrorDetails] = jsonFormat1(ServerErrorDetails)
}
