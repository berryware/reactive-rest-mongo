/*
 * Copyright (c) 2013-2015 Exaxis, LLC.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the distribution.
 * - Neither the name of the Exaxis, LLC nor the names of its contributors may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 */

package controllers

import play.api.mvc.Action

import scala.util.{Failure, Success}
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.Controller
import play.api.libs.json.{Format, Json}
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter}
import models.dao.IdentifiableDAO
import models._
import play.api.Logger
import scala.reflect.runtime.universe.TypeTag

/**
 * Created by dberry on 11/3/14.
 *
 * DAOController is a generic Controller that provides all the CRUD services for a domain object. The rest endpoints can call these methods to have
 * data sent to or retrieved from mongo collecions.
 *
 * Type param T must extend identifiable in order to be used by DAOController. Identifiable gives the class an id field that will be used as the key for
 * the CRUD Operations
 *
 */
trait DAOController[T <: Identifiable] extends Controller {
  import ExecutionContext.Implicits.global

  val dao:IdentifiableDAO[T]

  val defaultOffset = 0
  val defaultLimit = 25

  /**
   * Creates a T in a mongo collection.
   *
   * @param writer - The BSONDocumentWriter on the companion object for T
   * @param fmt - The Json.format[T] on the companion object for T
   * @return - Action[JsValue]
   */
  def create()(implicit writer: BSONDocumentWriter[T], fmt:Format[T] ) = Action.async(parse.json) { implicit request =>
    // process the json body
    request.body.validate[T].map { instance =>
      dao.insert(instance).map {
        case Failure(t) => BadRequest(t.getLocalizedMessage)
        case Success(count) => if (count == 0) Created else BadRequest
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  /**
   * Gets a T from the mongo collection using its id.
   *
   * @param id - id of the T to be retrieved
   * @param reader - The BSONDocumentReader on the companion object for T
   * @param fmt - The Json.format[T] on the companion object for T
   * @return - Action[AnyContent]
   */
  def getById(id:String)(implicit reader: BSONDocumentReader[T], fmt:Format[T]) = Action.async {
    dao.findById(Some(id)).map {
      case None => NotFound
      case Some(doc) => Ok(Json.toJson(doc))
    }.recover {
      case e: Exception => NotFound
    }
   }

  /**
   * Gets a T from the mongo collection using the url parameters as the keys
   *
   * @param reader - The BSONDocumentReader on the companion object for T
   * @param daoData - DaoSpecific data that must be passed along. Defined on the companion object for T
   * @param tag - Reflection info for T
   * @return - Action[AnyContent]
   */
  def getByAlt()(implicit reader: BSONDocumentReader[T], fmt:Format[T], daoData:DaoData[T], tag : TypeTag[T] ) = Action.async { request =>
    val paramMap = request.queryString.map {
          case (k, v) => Logger.debug(k+"->"+v); k -> v.mkString
    }
    dao.findOne(paramMap).map {
      case None => Logger.debug("Could not find by alternate attributes"); NotFound
      case Some(doc) => Ok(Json.toJson(doc))
    }.recover {
      case e: Exception => Logger.error("Errored out: "+e.getLocalizedMessage); NotFound
    }
   }

  /**
   * Updates a T based on its id. It retrieves the T from the request object.
   *
   * @param writer - The BSONDocumentWriter on the companion object for T
   * @param fmt - The Json.format[T] on the companion object for T
   * @return - Action[JsValue]
   */
  def update()(implicit writer: BSONDocumentWriter[T], fmt:Format[T]) = Action.async(parse.json) { request =>
      // process the json body
      request.body.validate[T].map { instance =>
        dao.update(instance).map {
          case Failure(t) => BadRequest(t.getLocalizedMessage)
          case Success(count) => if (count > 0) Accepted else NotFound
        }
      }.getOrElse(Future.successful(BadRequest("invalid json")))
    }

  /**
   * Deletes a T based on its id.
   *
   * @param id - id of the T to be deleted
   * @return - Action[AnyContent] which is a OK or NOT FOUND
   */
  def delete(id:String) = Action.async {
    dao.remove(Some(id)).map {
      case Failure(t) => BadRequest(t.getLocalizedMessage)
      case Success(count) => if (count > 0) Ok else NotFound
    }
  }

  /**
   * Deletes documents where the query matches data in the filter set
   *
   * @param q - A query that gets turned into a regex, and searches all fields listed in the DAOs Filter Set
   * @param daoData - DaoSpecific data that must be passed along. Defined on the companion object for T
   * @param tag - Reflection info for T
   * @return - Action[AnyContent] containingg the count of documents deleted or an error message
   */
  def deleteList(q: Option[String])(implicit daoData:DaoData[T], tag : TypeTag[T] ) = Action.async { request =>
    val paramMap = request.queryString.map {
      case (k, v) => Logger.debug(k+"->"+v); k -> v.mkString
    } - "q"

    dao.remove(q, paramMap).map {
      case Failure(t) => BadRequest(t.getLocalizedMessage)
      case Success(count) => Ok(count.toString)
    }
  }

  /**
   * Retrieves a single page list of T.
   *
   * @param p - The page that is being requested.
   * @param ipp - Items per page
   * @param q - A query that gets turned into a regex, .*q.* this gets used from search pages, and searches all fields listed in the DAOs Filter Set
   * @param s - This is the sort order. It is in the format of field1,field2 1, field3 -1. Which is field1 asc, field2 desc, field3 asc
   * @param reader - The BSONDocumentReader on the companion object for T
   * @param fmt - The Json.format[T] on the companion object for T
   * @param pgfmt - The Json.format[Pagination] on the companion object for Pagination
   * @param daoData - DaoSpecific data that must be passed along. Defined on the companion object for T
   * @param tag - Reflection info for T
   * @return - Action[AnyContent]
   */
  def list(p:Int, ipp: Int, q: Option[String], s: Option[String])(implicit reader: BSONDocumentReader[T], fmt: Format[T], pgfmt: Format[Pagination], daoData:DaoData[T], tag : TypeTag[T] ) = Action.async {
    request => // get all the params that are not know params
      val paramMap = request.queryString.map {
        case (k, v) => Logger.debug(k+"->"+v); k -> v.mkString
      } - "q" - "s" - "p" - "ipp"

      Logger.debug("q = "+q)
      Logger.debug("s = "+s)
      Logger.debug("p = "+p)
      Logger.debug("ipp = "+ipp)

      dao.find(q, s, p, ipp, paramMap).map {
        docs =>
          Ok(Json.toJson(Map("page" -> Json.toJson(new Pagination(p,ipp,docs._2)), "items" -> Json.toJson(docs._1))))
      }
  }

}
