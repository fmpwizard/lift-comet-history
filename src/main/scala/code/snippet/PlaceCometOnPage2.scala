package com.fmpwizard.code
package snippet


import scala.xml.NodeSeq

import net.liftweb._
import util._
import actor._
import http._
import Helpers._
import common.{Full, Logger, Box, Empty, Failure}

/**
  * This object adds a ComeActor of type Myliftactor with a name == random string
  * This allows having multiple tabs open displaying data for different contexts
  */
object PlaceCometOnPage2 extends Logger{

  def render(xhtml: NodeSeq): NodeSeq = {
    /**
     * You can set the id of the comet actor to be something you know the
     * value in advance. Using something like S.param("query_param")
     * or using the Menu.Param technique.
     *
     * In our case we just want a random name
     *
     */
    val id = Helpers.nextFuncName
    info("The current cometActor name is %s".format(id))


    for (sess <- S.session) sess.sendCometActorMessage("Myliftactor2", Full(id), Full(id))
    <lift:comet type="Myliftactor2" name={id}>{xhtml}</lift:comet>

  }
}