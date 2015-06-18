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

package models.dao

import reactivemongo.bson._

/* Implicits */

/**
 * Query builder wrapping common queries and MongoDB operators.
 *
 * TODO: create a real query `builder`
 *
 * @author      Pedro De Almeida (almeidap)
 */
object DBQueryBuilder {

	/**
	 * Convert a BSONObjectID to a BSONDocument containing the _id field name and the BSONObjectID.
	 *
	 * @param objectId - The BSONObjectID
	 * @return - a BSONDocument
	 */
	def id(objectId: BSONObjectID): BSONDocument = BSONDocument("_id" -> objectId)

	/**
	 * Convert an Option[String] to a BSONDocument containing the _id field name and the BSONObjectID.
	 * If the Option[String] is None it sets _id to BSONUndefined
	 *
	 * @param objectId
	 * @return - a BSONDocument
	 */
  def id(objectId: Option[String]): BSONDocument = objectId match {
     case None => BSONDocument("_id" -> BSONUndefined)
     case Some(s) => id(BSONObjectID(s))
   }

	/**
	 * Set a BSONDocument containing the field and BSONDocument data
	 *
	 * @param field  - The string name of the field to be set
	 * @param data - The BSONDocument data to be set for the specific field
	 * @return - a BSONDocument
	 */
	def set(field: String, data: BSONDocument): BSONDocument = set(BSONDocument(field -> data))

	/**
	 * Set a BSONDocument containing the field and T data
	 *
	 * @param field - The string name of the field to be set
	 * @param data - The T data to be set for the specific field
	 * @param writer - The BSONDocumentWriter for mashalling the T to a BSONDocument
	 * @tparam T - The type parameter T
	 * @return - a BSONDocument
	 */
	def set[T](field: String, data: T)(implicit writer: BSONDocumentWriter[T]): BSONDocument = set(BSONDocument(field -> data))

	/**
	 * Sets the data
	 *
	 * @param data - The T data to be set
	 * @return - a BSONDocument
	 */
	def set(data: BSONDocument): BSONDocument = BSONDocument("$set" -> data)

	/**
	 * Sets the data
	 *
	 * @param data - The T data to be set
	 * @param writer - The BSONDocumentWriter for mashalling the T to a BSONDocument
	 * @tparam T - The type parameter T
	 * @return - a BSONDocument
	 */
	def set[T](data: T)(implicit writer: BSONDocumentWriter[T]): BSONDocument = BSONDocument("$set" -> data)

	/**
	 * Push a BSONDocument containing the field and the data
	 *
	 * @param field - The string name of the field to push
	 * @param data - The T data to be assigned to the field to push
	 * @param writer - The BSONDocumentWriter for mashalling the T to a BSONDocument
	 * @tparam T - The type parameter T
	 * @return - a BSONDocument
	 */
	def push[T](field: String, data: T)(implicit writer: BSONDocumentWriter[T]): BSONDocument = BSONDocument("$push" -> BSONDocument(field -> data))

	/**
	 *
	 * @param field - The string name of the field to pull
	 * @param query
	 * @param writer - The BSONDocumentWriter for mashalling the T to a BSONDocument
	 * @tparam T - The type parameter T
	 * @return - a BSONDocument
	 */
	def pull[T](field: String, query: T)(implicit writer: BSONDocumentWriter[T]): BSONDocument = BSONDocument("$pull" -> BSONDocument(field -> query))

	/**
	 *
	 * @param field - The string name of the field to be unset
	 * @return - a BSONDocument
	 */
	def unset(field: String): BSONDocument = BSONDocument("$unset" -> BSONDocument(field -> 1))

	/**
	 * Increment a specific field by a specific amount
	 *
	 * @param field - The string name of the field to be incremented
	 * @param amount - The amount to add to the field
	 * @return - a BSONDocument
	 */
	def inc(field: String, amount: Int) = BSONDocument("$inc" -> BSONDocument(field -> amount))

}