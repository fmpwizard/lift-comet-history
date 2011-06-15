package com.fmpwizard.code
package comet


import scala.xml.{NodeSeq, Text, Elem}

import net.liftweb._
import util._
import actor._
import http._
import common.{Box, Full,Logger}
import http.SHtml._
import http.S._
import http.js.JsCmds.{SetHtml, SetValueAndFocus, Replace}
import http.js.jquery.JqJE._
import net.liftweb.http.js.JE.Str
import Helpers._

import api.CityStateUpdate

/**
 * This is the message we pass around to
 * register each named comet actor with a dispatcher that
 * only updates the specific browser tab it is on
 */
case class registerCometActor(actor: CometActor, name: String)

class Myliftactor extends CometActor with Logger {

  override def defaultPrefix = Full("comet")

  // time out the comet actor if it hasn't been on a page for 2 minutes
  override def lifespan = Full(120 seconds)

  /**
   * On page load, this method does a full page render.
   * We store the comet actor name on a hidden inout field.
   */
  def render= {
    "#cometName [value]" #> name
  }

  /**
   * We can get two kinds of messages
   * 1- A CityStateUpdate, which has info about the city
   * and State we will update.
   * The REST API sends this message
   *
   * 2- A string which is the name of the comet actor that
   * we need to send updates to.
   *
   */
  override def lowPriority: PartialFunction[Any,Unit] = {
    case CityStateUpdate(cometName, city, state) => {
      info("Comet Actor %s will do a partial update".format(this))

      /**
       * You can have many partialUpdate() calls here.
       */
      partialUpdate(
        SetHtml("city", Text(city))
      )
      partialUpdate(
        SetHtml("state", Text(state))
      )
    }
    case Full(name: String)=> {
      info("[URL]: CometActor monitoring session: %s".format(name))

      /**
       * We get the DispatcherActor that sends message to all the
       * CometActors that are on a tab.
       * And we register ourselves with the dispatcher
       */

      MyListeners.listenerFor(name) ! registerCometActor(this, name)
      info("Registering comet actor: %s".format(this))

    }
    case _ => info("Not sure how we got here.")
  }


}

/**
 * This class keeps a list of comet actors that need to update the UI
 * if we get new data through the rest api
 */
class DispatcherActor(name: String) extends LiftActor  with Logger{

  private var cometActorsToUpdate: List[CometActor]= List()

  override def messageHandler  = {
    /**
     * if we do not have this actor in the list, add it (register it)
     */
    case registerCometActor(actor, name) =>
      if(cometActorsToUpdate.contains(actor) == false){
        info("We are adding actor: %s to the list".format(actor))
        cometActorsToUpdate= actor :: cometActorsToUpdate
      } else {
        info("The list so far is %s".format(cometActorsToUpdate))
      }

    /**
     * Go through the the list of actors and send them a CityStateUpdate message
     */
    case CityStateUpdate(cometName, city, state) => {
      info("We will update these comet actors: %s showing name: %s".format(
        cometActorsToUpdate, cometName))
      cometActorsToUpdate.foreach(_ ! CityStateUpdate(cometName, city, state))
    }
    case _ => "We got a strange message, sorry."
  }

}


/**
 * Keep a map of cometName -> dispatchers, if no dispatcher is found, create one
 * comet actors get the ref to their dispatcher using this object,
 * so they can register themselves and the rest
 * api gets the dispatcher that will update a specific browser tab
 *
 */
object MyListeners extends Logger{

  private var listeners: Map[String, LiftActor] = Map()

  def listenerFor(str: String): LiftActor = synchronized {
    listeners.get(str) match {
      case Some(a) => info("Our map is %s".format(listeners)); a
      case None => {
        val ret = new DispatcherActor(str)
        listeners += str -> ret
        info("Our map is %s".format(listeners))
        ret
      }
    }
  }

}