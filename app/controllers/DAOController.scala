/*
 * Copyright (c) 2013-2014 Exaxis, LLC.
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

import scala.util.{Failure, Success}
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.{Action,Controller}
import play.api.libs.json.{Format, Json}
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter}
import models.dao.IdentifiableDAO
import models.{FilterSet, Pagination, AttributeMap, Identifiable}
import play.api.Logger
import scala.reflect.runtime.universe.TypeTag

/**
 * Created by dberry on 11/3/14.
 */
class DAOController[T <: Identifiable] extends Controller {
  import ExecutionContext.Implicits.global

  val defaultOffset = 0
  val defaultLimit = 25

  def create()(implicit dao:IdentifiableDAO[T], writer: BSONDocumentWriter[T], fmt:Format[T] ) = Action.async(parse.json) { request =>
    // process the json body
    request.body.validate[T].map { instance =>
      dao.insert(instance).map {
        case Failure(t) => BadRequest(t.getLocalizedMessage)
        case Success(count) => if (count == 0) Created else BadRequest
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def get(id:String)(implicit dao:IdentifiableDAO[T],reader: BSONDocumentReader[T], fmt:Format[T]) = Action.async {
    dao.findById(id).map {
      case None => NotFound
      case Some(funnler) => Ok(Json.toJson(funnler))
    }.recover {
      case e: Exception => NotFound
    }
   }

  def update()(implicit dao:IdentifiableDAO[T], writer: BSONDocumentWriter[T], fmt:Format[T]) = Action.async(parse.json) { request =>
      // process the json body
      request.body.validate[T].map { instance =>
        dao.update(instance).map {
          case Failure(t) => BadRequest(t.getLocalizedMessage)
          case Success(count) => if (count > 0) Accepted else NotFound
        }
      }.getOrElse(Future.successful(BadRequest("invalid json")))
    }

  def delete(id:String)(implicit dao:IdentifiableDAO[T]) = Action.async {
    dao.remove(id).map {
      case Failure(t) => BadRequest(t.getLocalizedMessage)
      case Success(count) => if (count > 0) Ok else NotFound
    }
  }

  def deleteList(q: Option[String])(implicit dao:IdentifiableDAO[T], filterSet: FilterSet[T], attrMap: AttributeMap[T], tag : TypeTag[T] ) = Action.async { request =>
    val paramMap = request.queryString.map {
      case (k, v) => Logger.debug(k+"->"+v); k -> v.mkString
    } - "q"

    dao.remove(q, paramMap).map {
      case Failure(t) => BadRequest(t.getLocalizedMessage)
      case Success(count) => if (count > 0) Ok(count.toString) else NotFound
    }
  }

  def list(p:Int, ipp: Int, q: Option[String], s: Option[String])(implicit dao: IdentifiableDAO[T], reader: BSONDocumentReader[T], fmt: Format[T], pgfmt: Format[Pagination], filterSet: FilterSet[T], attrMap: AttributeMap[T], tag : TypeTag[T] ) = Action.async {
    request =>
      val paramMap = request.queryString.map {
        case (k, v) => Logger.debug(k+"->"+v); k -> v.mkString
      } - "q" - "s" - "p" - "ipp"

      Logger.debug("q = "+q)
      Logger.debug("s = "+s)
      Logger.debug("p = "+p)
      Logger.debug("ipp = "+ipp)

      dao.find(q, s, p, ipp, paramMap).map {
        funnlers =>
          Ok(Json.toJson(Map("page" -> Json.toJson(new Pagination(p,ipp,funnlers._2)), "items" -> Json.toJson(funnlers._1))))
      }
  }

}
