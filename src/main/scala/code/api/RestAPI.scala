package com.fmpwizard.code
package api


import scala.xml.{Elem, Node, NodeSeq, Text}

import net.liftweb.common.{Box,Empty,Failure,Full,Logger}
import net.liftweb.http.rest.RestHelper
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.json._
import net.liftweb.actor._
import net.liftweb.http._
import net.liftweb.util.Helpers._

import comet.MyListeners._


case class CityStateUpdate(CometName: String, City: String, State: String)

object RestHelperAPI extends RestHelper with Logger {

  /**
   * This case class is use to easily parse the json text we get through the REST API
   */
  case class JsonExtractor(
    comet_name: Option[String],
    id: Option[String]
  )

  /**
   * The heart of the rest api, we listen for urls like:
   * http://hostname/v1/rest/cities/id
   */
  serve {
         ///v1/     rest/     cities/    1.json
    case "v1" :: "rest" :: "cities" :: _ JsonPut jsonData -> _ =>
      // jsonData is a net.liftweb.json.JsonAST.JValue
      verifyJsonPutData(Full(jsonData.extract[JsonExtractor]))
  }

  /**
   * See if the json matches your requirements
   * If so, update the UI
   *
   */
  def verifyJsonPutData(parsedJsonPutData: Box[JsonExtractor]): LiftResponse=
    parsedJsonPutData match {
    case Full(jsonPutData) => {
      jsonPutData match {
      /**
       * If we have all fields, add them to the database
       */

        case JsonExtractor(
          Some(comet_name), Some(id)) => {
            debug("Parsing of json complete for id: %s".format(id))

            /**
             * Based on the id, get a city -> State pair
             */
            val cityMap= lib.CitiesAndStates.cityStateMap(toInt(id))
            info(cityMap)
            val lookedupCity  = cityMap(0)._1
            val lookedupState = cityMap(0)._2




          /**
           * Tell the BrowserDetails comet actor to update the UI
           */

            debug(
              "REST API will send an update to actor: %s: ".format(listenerFor(comet_name))
            )

          /**
           * listenerFor(comet_name) returns a DispatcherActor that in turn
           * will send the CityStateUpdate case class to the correct comet actors that
           * we got json data for
           */
            listenerFor(comet_name) match {
              case a: LiftActor => a !
                CityStateUpdate(comet_name, lookedupCity, lookedupState)
              case _ => info("No actor to send an update")
            }
            debug("We will update city: %s, state: %s".format(lookedupCity, lookedupState))

            NoContentResponse()
        }
        // Else. log an error and return a erro4 400 with a message
        case JsonExtractor(a, b) => {
          info("It did not passed: %s".format(a))
          val msg= ("We are missing some fields, " +
            "we got: %s").format(a)
          ResponseWithReason(BadResponse(), msg)
        }
      }
    }
    case Failure(msg, _, _) => {
      info(msg)
      ResponseWithReason(BadResponse(), msg)
    }
    case error => {
      info("Parsed browserTestResult as : " + error)
      BadResponse()
    }
  }



}
