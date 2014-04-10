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

package models

import org.joda.time.DateTime
import models.dao.IdentifiableDAO
import reactivemongo.bson._
import reactivemongo.bson.BSONDateTime

case class User (
  id : String,
  firstName: String,
  lastName: String,
  fullName: String,
  age:Option[Int],
  email: Option[String],
  avatarUrl: Option[String],
  created : Option[DateTime],
  updated : Option[DateTime]
) extends Temporal

object User {
  import play.api.libs.json._

  implicit val format = Json.format[User]

  implicit object DAO extends IdentifiableDAO[User]{
    val collectionName = "myusers"
  }

  /**
   * defines the attributes that will be used in a filtered search
   */
  implicit object UserFilterSet extends FilterSet[User] {
    val filterSet = Set (
      "firstName",
      "lastName",
      "fullName"
    )
  }

  /**
   * defines the mapping of scala attributes to datastore attributes, same named attributes do not need to be mapped
   */
  implicit object UserAttributeMap extends AttributeMap[User] {
    val attributeMap = Map (
      "id" -> "_id"
    )
  }

  implicit object UserBSONReader extends BSONDocumentReader[User] {
    def read(doc: BSONDocument): User =
      User(
        doc.getAs[BSONObjectID]("_id").get.stringify,
        doc.getAs[String]("firstName").get,
        doc.getAs[String]("lastName").get,
        doc.getAs[String]("fullName").get,
        doc.getAs[Int]("age"),
        doc.getAs[String]("email"),
        doc.getAs[String]("avatarUrl"),
        doc.getAs[BSONDateTime]("created").map(dt => new DateTime(dt.value)),
        doc.getAs[BSONDateTime]("updated").map(dt => new DateTime(dt.value))
      )
  }

  implicit object UserBSONWriter extends BSONDocumentWriter[User] {
    def write(user: User): BSONDocument =
      BSONDocument(
        "_id" -> BSONObjectID(user.id),
        "firstName" -> user.firstName,
        "lastName" -> user.lastName,
        "fullName" -> user.fullName,
        "age" -> user.age,
        "email" -> user.email,
        "avatarUrl" -> user.avatarUrl,
        "created" -> user.created.map(date => BSONDateTime(date.getMillis)),
        "updated" -> BSONDateTime(DateTime.now.getMillis)
      )
  }



}
