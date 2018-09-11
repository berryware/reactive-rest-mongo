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

package models

import org.exaxis.smd.{Identifiable, DaoData}
import org.joda.time.DateTime
import reactivemongo.bson._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

/**
 * The case class for the User Data access object. It needs to extend Identifiable or Temporal to be usable by the
 * reactive mongo wrapper.
 *
 * @param id
 * @param firstName
 * @param lastName
 * @param fullName
 * @param age
 * @param email
 * @param avatarUrl
 * @param created
 * @param updated
 */
case class User (
  id : Option[String],
  firstName: String,
  lastName: String,
  fullName: String,
  age:Option[Int],
  email: Option[String],
  avatarUrl: Option[String],
  created : Option[DateTime],
  updated : Option[DateTime]
) extends Identifiable

/**
 * This is the companion object for the User case class and contains all the additional mojo needed by the reactive mongo wrapper.
 *
 * By mojo we mean that it defines all the implicits that are needed for all the implicit parameters that are defined in all the
 * downstream libraries.
 *
 */
object User {
  import play.api.libs.json._

  /**
   * String attribute names for the scala class
   */
  val ATTR_ID = "id"
  val ATTR_FIRSTNAME = "firstName"
  val ATTR_LASTNAME = "lastName"
  val ATTR_FULLNAME = "fullName"
  val ATTR_AGE = "age"
  val ATTR_EMAIL = "email"
  val ATTR_AVATARURL = "avatarUrl"
  val ATTR_CREATED = "created"
  val ATTR_UPDATED = "updated"

  /**
   * String attribute names in the data store
   */
  val DS_ID = "_id"
  val DS_AGE = "_age"

  /**
   * The JSON Formatter needed by the BSONReader and BSONWriter
   */
  implicit val format = Json.format[User]

  implicit val daoData = new DaoData[User] {
    /**
     * defines the attributes that will be matched against a query in the search.
     */
    val filterSet = Set(
      ATTR_FIRSTNAME,
      ATTR_LASTNAME,
      ATTR_FULLNAME
    )

    /**
     * defines the mapping of scala attributes to datastore attributes, same named attributes do not need to be mapped
     */
    val attributeMap = Map (
      ATTR_ID -> DS_ID,
      ATTR_AGE -> DS_AGE
    )
  }

  /**
   * Marshalls a BSONDocument into a User
   *
   */
  implicit val UserBSONReader = new BSONDocumentReader[User] {
    def read(doc: BSONDocument): User =
      User(
        doc.getAs[BSONObjectID](daoData.dsName(ATTR_ID)) map { _.stringify},
        doc.getAs[String](daoData.dsName(ATTR_FIRSTNAME)).get,
        doc.getAs[String](daoData.dsName(ATTR_LASTNAME)).get,
        doc.getAs[String](daoData.dsName(ATTR_FULLNAME)).get,
        doc.getAs[Int](daoData.dsName(ATTR_AGE)),
        doc.getAs[String](daoData.dsName(ATTR_EMAIL)),
        doc.getAs[String](daoData.dsName(ATTR_AVATARURL)),
        doc.getAs[BSONDateTime](daoData.dsName(ATTR_CREATED)).map(dt => new DateTime(dt.value)),
        doc.getAs[BSONDateTime](daoData.dsName(ATTR_UPDATED)).map(dt => new DateTime(dt.value))
      )
  }

  /**
   * Marshalls a User into a BSONDocument.
   *
   * Never write the id out. It is handled by the dao
   */
  implicit val UserBSONWriter = new BSONDocumentWriter[User] {
    def write(user: User): BSONDocument =
      BSONDocument(
        daoData.dsName(ATTR_FIRSTNAME) -> user.firstName,
        daoData.dsName(ATTR_LASTNAME) -> user.lastName,
        daoData.dsName(ATTR_FULLNAME) -> user.fullName,
        daoData.dsName(ATTR_AGE) -> user.age,
        daoData.dsName(ATTR_EMAIL) -> user.email,
        daoData.dsName(ATTR_AVATARURL) -> user.avatarUrl,
        daoData.dsName(ATTR_CREATED) -> user.created.map(date => BSONDateTime(date.getMillis)),
        daoData.dsName(ATTR_UPDATED) -> BSONDateTime(DateTime.now.getMillis)
      )
  }

}
