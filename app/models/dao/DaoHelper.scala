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

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}
import reactivemongo.core.commands.LastError

/**
 * DaoHelper contains the error handling used by all the DAO classes. The idea is that whether it is mongo,
 * cassandra, or any other database. The error handling should be the same.
 *
 * @author      Pedro De Almeida (almeidap)
 */
trait DaoHelper {

  implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

  /**
   * Execute a function that returns a Future[LastError] and return a Future[Try[Int]]. The Int that is returned
   * is the number of documents, records, etc that were effected by the operation.
   *
   * @param operation - a function that returns a Future[LastError]
   * @return - a Future[Try[Int]].
   */
  // TODO: Need to replace LastError with something that is not Mongo specific
  def tryIt(operation: Future[LastError]): Future[Try[Int]] = operation.map {
    lastError =>
      lastError.inError match {
        case true => Failure(lastError.getCause)
        case false => Success(lastError.updated)
      }
  } recover {
    case throwable => Failure(throwable)
  }

}
