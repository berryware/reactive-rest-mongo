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

import reactivemongo.bson._

import scala.concurrent.Future
import scala.util.Try
import models.Identifiable

/**
 * Created by dberry on 7/3/14.
 */
trait IdentifiableDAO[T <: Identifiable] extends MongoDao[T] {

  def update(document: T)(implicit writer: BSONDocumentWriter[T]): Future[Try[Int]] = super.update(document.id, document)

  def remove(document: T): Future[Try[Int]] = super.remove(document.id)

  // TODO: look to make deletes faster by id $in list
  def remove(documents: List[T]): Future[List[Try[Int]]] = Future.sequence(documents.map{ document => super.remove(document.id) })

  def update(documents: List[T])(implicit writer: BSONDocumentWriter[T]): Future[List[Try[Int]]] = Future.sequence(documents.map{ document => super.update(document.id, document) })

  def findById(document: T)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = super.findById(document.id)

}
