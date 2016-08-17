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

import org.joda.time.DateTime
import models.dao.IdentifiableDAO
import reactivemongo.bson._
import reactivemongo.bson.BSONDateTime

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
) extends Temporal

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
  val attrId = "id"
  val attrFirstName = "firstName"
  val attrLastName = "lastName"
  val attrFullName = "fullName"
  val attrAge = "age"
  val attrEmail = "email"
  val attrAvatarUrl = "avatarUrl"
  val attrCreated = "created"
  val attrUpdated = "updated"

  /**
   * String attribute names in the data store
   */
  val dsId = "_id"
  val dsAge = "_age"

  /**
   * The JSON Formatter needed by the BSONReader and BSONWriter
   */
  implicit val format = Json.format[User]

  implicit val userDaoData = new DaoData[User] {
    /**
     * defines the attributes that will be matched against a query in the search.
     */
    val filterSet = Set(
      attrFirstName,
      attrLastName,
      attrFullName
    )

    /**
     * defines the mapping of scala attributes to datastore attributes, same named attributes do not need to be mapped
     */
    val attributeMap = Map (
      attrId -> dsId,
      attrAge -> dsAge
    )
  }

  /**
   * Marshalls a BSONDocument into a User
   *
   */
  implicit val UserBSONReader = new BSONDocumentReader[User] {
    def read(doc: BSONDocument): User =
      User(
        doc.getAs[BSONObjectID](userDaoData.dataSourceName(attrId)) map { _.stringify},
        doc.getAs[String](userDaoData.dataSourceName(attrFirstName)).get,
        doc.getAs[String](userDaoData.dataSourceName(attrLastName)).get,
        doc.getAs[String](userDaoData.dataSourceName(attrFullName)).get,
        doc.getAs[Int](userDaoData.dataSourceName(attrAge)),
        doc.getAs[String](userDaoData.dataSourceName(attrEmail)),
        doc.getAs[String](userDaoData.dataSourceName(attrAvatarUrl)),
        doc.getAs[BSONDateTime](userDaoData.dataSourceName(attrCreated)).map(dt => new DateTime(dt.value)),
        doc.getAs[BSONDateTime](userDaoData.dataSourceName(attrUpdated)).map(dt => new DateTime(dt.value))
      )
  }

  /**
   * Marshalls a User into a BSONDocument
   *
   */
  implicit val UserBSONWriter = new BSONDocumentWriter[User] {
    def write(user: User): BSONDocument =
      BSONDocument(
        userDaoData.dataSourceName(attrId) -> user.id.map(BSONObjectID(_)),
        userDaoData.dataSourceName(attrFirstName) -> user.firstName,
        userDaoData.dataSourceName(attrLastName) -> user.lastName,
        userDaoData.dataSourceName(attrFullName) -> user.fullName,
        userDaoData.dataSourceName(attrAge) -> user.age,
        userDaoData.dataSourceName(attrEmail) -> user.email,
        userDaoData.dataSourceName(attrAvatarUrl) -> user.avatarUrl,
        userDaoData.dataSourceName(attrCreated) -> user.created.map(date => BSONDateTime(date.getMillis)),
        userDaoData.dataSourceName(attrUpdated) -> BSONDateTime(DateTime.now.getMillis)
      )
  }

}
