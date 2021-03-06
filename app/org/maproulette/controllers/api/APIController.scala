package org.maproulette.controllers.api

import javax.inject.Inject

import org.maproulette.exception.{StatusMessage, StatusMessages}
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{Action, Controller}

/**
  * A basic action controller for miscellaneous operations on the API
  *
  * @author cuthbertm
  */
class APIController @Inject() extends Controller with StatusMessages {

  /**
    * In the routes file this will be mapped to any /api/v2/ paths. It is the last mapping to take
    * place so if it doesn't match any of the other routes it will fall into this invalid path.
    *
    * @param path The path found after /api/v2
    * @return A json object returned with a 400 BadRequest
    */
  def invalidAPIPath(path:String) = Action {
    BadRequest(Json.toJson(StatusMessage("KO", JsString(s"Invalid Path [$path] for API"))))
  }
}
