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
import scaldi.Injector

import scala.concurrent.Future
import scala.util.Try
import models.Identifiable

/**
 * IdentifiableDAO is a trait that adds methods to MongoDAO that work with Identifiable.
 *
 * It uses the id attribute of the Identifiable to simplifiy and execute methods that take an id.
 *
 * @author dberry
 */
abstract class IdentifiableDAO[T <: Identifiable] extends MongoDao[T] {

  /**
   * Update a T document in a mongo collection
   *
   * @param document - a T
   * @param writer - The BSONDocumentWriter on the companion object for T
   * @return - Future[Try[Int] ]
   */
  def update(document: T)(implicit writer: BSONDocumentWriter[T]): Future[Try[Int]] = super.update(document.id, document)

  /**
   * Update a list of T documents in a mongo collection
   *
   * @param documents - a list of T
   * @param writer - The BSONDocumentWriter on the companion object for T
   * @return - Future[Try[Int] ]
   */
  def update(documents: List[T])(implicit writer: BSONDocumentWriter[T]): Future[List[Try[Int]]] = Future.sequence(documents.map{ document => super.update(document.id, document) })

  /**
   * Remove a T document in a mongo collection
   *
   * @param document - a T
   * @return - Future[Try[Int] ]
   */
  def remove(document: T): Future[Try[Int]] = super.remove(document.id)

  /**
   * Remove a list of T documents in a mongo collection
   *
   * @param documents - a list of T
   * @return - Future[Try[Int] ]
   */
  // TODO: look to make deletes faster by id $in list
  def remove(documents: List[T]): Future[List[Try[Int]]] = Future.sequence(documents.map{ document => super.remove(document.id) })

  /**
   * Find a T by its id
   *
   * @param document - a T
   * @param reader - The BSONDocumentReader on the companion object for T
   * @return - Future[Try[Int] ]
   */
  def findById(document: T)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = super.findById(document.id)

}
