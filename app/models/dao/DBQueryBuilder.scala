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

package models.dao

import reactivemongo.bson.{BSONDocumentWriter, BSONDocument, BSONObjectID}

/* Implicits */

/**
 * Query builder wrapping common queries and MongoDB operators.
 *
 * TODO: create a real query `builder`
 *
 * @author      Pedro De Almeida (almeidap)
 */
object DBQueryBuilder {

	def id(objectId: String): BSONDocument = id(BSONObjectID(objectId))

	def id(objectId: BSONObjectID): BSONDocument = BSONDocument("_id" -> objectId)

	def set(field: String, data: BSONDocument): BSONDocument = set(BSONDocument(field -> data))

	def set[T](field: String, data: T)(implicit writer: BSONDocumentWriter[T]): BSONDocument = set(BSONDocument(field -> data))

	def set(data: BSONDocument): BSONDocument = BSONDocument("$set" -> data)

	def set[T](data: T)(implicit writer: BSONDocumentWriter[T]): BSONDocument = BSONDocument("$set" -> data)

	def push[T](field: String, data: T)(implicit writer: BSONDocumentWriter[T]): BSONDocument = BSONDocument("$push" -> BSONDocument(field -> data))

	def pull[T](field: String, query: T)(implicit writer: BSONDocumentWriter[T]): BSONDocument = BSONDocument("$pull" -> BSONDocument(field -> query))

	def unset(field: String): BSONDocument = BSONDocument("$unset" -> BSONDocument(field -> 1))

	def inc(field: String, amount: Int) = BSONDocument("$inc" -> BSONDocument(field -> amount))

}