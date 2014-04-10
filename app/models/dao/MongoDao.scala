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

import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson._
import scala.concurrent.Future
import scala.util.{Try, Success, Failure}
import play.api.Logger
import reactivemongo.core.commands.Count
import models._
import play.api.i18n.Messages
import reactivemongo.bson.{BSONBoolean,BSONDouble,BSONString,BSONLong,BSONInteger,BSONRegex}
import scala.Some
import reactivemongo.api.QueryOpts
import reactivemongo.api.collections.default.BSONCollection
import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.Type
import org.joda.time.DateTime

/**
 * Created by dberry on 13/3/14.
 */
trait MongoDao[T] extends DaoHelper {

  val collectionName: String

  import play.api.Play.current

  def collection = ReactiveMongoPlugin.db.collection[BSONCollection](collectionName)

  def insert(document: T)(implicit writer: BSONDocumentWriter[T]): Future[Try[Int]] = {
    Logger.debug(s"Inserting document: [collection=$collectionName, data=$document]")
    tryIt(collection.insert(document))
  }

  def findById(id: String)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = findOne(DBQueryBuilder.id(id))

  def findById(id: BSONObjectID)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = findOne(DBQueryBuilder.id(id))

  def findAll(query: BSONDocument = BSONDocument.empty, sort: BSONDocument = BSONDocument.empty)(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    Logger.debug(s"Finding documents: [collection=$collectionName, query=$query]")
    collection.find(query).sort(sort).cursor[T].collect[List]()
  }

  def find(filter: Option[String], orderby: Option[String], page: Int, ipp: Int, params: Map[String, String])(implicit reader: BSONDocumentReader[T], filterSet:FilterSet[T], attrMap:AttributeMap[T], tag : TypeTag[T] ): Future[(List[T], Int)] = {

    val query = buildQueryDocument(filter, params, filterSet, attrMap)

    val sort:BSONDocument = orderby match {
      case None => Logger.debug("orderby = None"); BSONDocument.empty
      case Some(sorts) => Logger.debug("orderby = "+sorts);
        sorts.split(",").foldLeft(BSONDocument.empty) {
          (doc, s) =>
            s.split(" ").toList match {
              case a :: Nil => doc ++ (attrMap.attributeMap.getOrElse(a, a) -> 1)
              case a :: b :: Nil => doc ++ (attrMap.attributeMap.getOrElse(a, a) -> b.toInt)
              case _ => doc ++ ()
            }
        }
    }

    // Execute the query
    find(query, sort, page, ipp)
  }

  private def buildQueryDocument(filter: Option[String], params: Map[String, String], filterSet:FilterSet[T], attrMap:AttributeMap[T])(implicit tag : TypeTag[T] ) = {
    BSONDocument( "$and" -> (BSONArray.empty ++
    (filter match {
      case None => Logger.debug("q = None"); BSONDocument.empty
      case Some(s) => Logger.debug("q = "+s)
        BSONDocument( "$or" -> filterSet.filterSet.foldLeft(BSONArray.empty) { (arr, attr) => arr ++  BSONDocument(attr -> BSONRegex(".*" + s + ".*", "i")) })
    }) ++ buildAttributeDocument(params, attrMap) ) )
  }

  private def buildAttributeDocument(params: Map[String, String], attrMap:AttributeMap[T] )(implicit tag : TypeTag[T] ) = {
    params.keys.foldLeft(BSONDocument.empty) {
      (doc,key) => doc ++ processAttribute(key, params.get(key).get, attrMap.attributeMap.getOrElse(key, key))
    }
  }

  /**
   * Process attribute turns query parameters and values into searchable objects within mongo. Depending on the format of sval, we will convert
   * name and sval into an exact match, an in, or a range
   *
   * ?datefield=12341234    converts to exact match datefield -> BSONDateTime(123412134)
   * ?intfield=60           converts to exact match intfield -> BSONInteger(60)
   * ?intfield=[60, 70, 80] converts to in match    intfield ->
   * ?intfield=(60,)        converts to $gte match  intfield ->
   * ?intfield=(,70)        converts to $lte match  intfield ->
   * ?intfield=(60,70)      converts to range match intfield ->
   *
   * @param name
   * @param sval
   * @param dbName
   * @param tag
   * @return
   */
  private def processAttribute(name:String, sval:String,  dbName:String)(implicit tag : TypeTag[T] ) : (String, BSONValue) = {
    sval.charAt(0) match {
      case '(' => processRange(name, sval.substring(1,sval.length-1), dbName) // process the range
      case '[' => processIn(name, sval.substring(1,sval.length-1), dbName)    // process the in
      case s => dbName -> convertAttributeToBson(name, sval)
    }
  }

  private def processRange(name:String, sval:String,  dbName:String)(implicit tag : TypeTag[T] ) : (String, BSONValue) = {
    val rangeArray = sval.split(",", -1)
    "$and" -> (BSONArray.empty ++ buildRangeBSON("$gte", name, rangeArray(0), dbName) ++ buildRangeBSON("$lte", name, rangeArray(1), dbName))
  }

  private def buildRangeBSON(symbol:String, name:String, sval:String,  dbName:String)(implicit tag : TypeTag[T] ) : BSONDocument = {
    if (sval.isEmpty)
      BSONDocument.empty
    else
      BSONDocument( name -> BSONDocument(symbol -> convertAttributeToBson(name, sval)))
  }

  private def processIn(name:String, sval:String,  dbName:String)(implicit tag : TypeTag[T] ) : (String, BSONValue) = {
    name -> BSONDocument( "$in" -> (sval.split(",").map{ inVal => convertAttributeToBson(name, inVal) }))
  }

  /**
   *  WARNING - This method uses EXPERIMENTAL reflection code from scala. Scala does not guarantee backwards compatibility in their releases.
   *  If you upgrade scala and this method no longer compiles then it MUST be fixed using whatever new classes, methods, etc that the scala team
   *  decided to go with. I would rather have this than living with things like type erasure for backwards compatibility. If you do not know what
   *  type erasure is then please put this code down, step away from the code, and let someone else smarter than you work on this code.
   */
  private def convertAttributeToBson(name:String, sval:String)(implicit tag : TypeTag[T] ) : BSONValue = {
      // use reflection to validate the attribute and to construct the right BSONValue
    tag.tpe.member(newTermName(name)) match {
      case NoSymbol => BSONUndefined
      case s => s.typeSignature match {
          case NullaryMethodType(tpe) if typeOf[Boolean] =:= tpe          => BSONBoolean(sval.toBoolean)
          case NullaryMethodType(tpe) if typeOf[Option[Boolean]] =:= tpe  => BSONBoolean(sval.toBoolean)
          case NullaryMethodType(tpe) if typeOf[Int] =:= tpe              => BSONInteger(sval.toInt)
          case NullaryMethodType(tpe) if typeOf[Option[Int]] =:= tpe      => BSONInteger(sval.toInt)
          case NullaryMethodType(tpe) if typeOf[Long] =:= tpe             => BSONLong(sval.toLong)
          case NullaryMethodType(tpe) if typeOf[Option[Long]] =:= tpe     => BSONLong(sval.toLong)
          case NullaryMethodType(tpe) if typeOf[Double] =:= tpe           => BSONDouble(sval.toDouble)
          case NullaryMethodType(tpe) if typeOf[Option[Double]] =:= tpe   => BSONDouble(sval.toDouble)
          case NullaryMethodType(tpe) if typeOf[DateTime] =:= tpe         => BSONDateTime(sval.toLong)
          case NullaryMethodType(tpe) if typeOf[Option[DateTime]] =:= tpe => BSONDateTime(sval.toLong)
          case _ => BSONRegex(sval.replaceAll("\\*", ".*"),"i")  // default is a String
        }
    }
  }

  def find(query: BSONDocument = BSONDocument.empty, sort: BSONDocument = BSONDocument.empty, page: Int, ipp: Int)(implicit reader: BSONDocumentReader[T]): Future[(List[T], Int)] = {
    val queryString = BSONDocument.pretty(query)
    val sortString = BSONDocument.pretty(sort)
    Logger.debug(s"Finding documents: [collection=$collectionName, query=$queryString] sort=$sortString")
    for {
      docs <- collection.find(query).sort(sort).options(QueryOpts((page-1)*ipp, ipp)).cursor[T].collect[List](ipp)
      totalDocs <- ReactiveMongoPlugin.db.command(Count(collectionName, Some(query)))
    } yield (docs, (totalDocs/ipp)+1)
  }

  def findOne(query: BSONDocument)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = {
    val queryString = BSONDocument.pretty(query)
    Logger.debug(s"Finding one: [collection=$collectionName, query=$queryString]")
    collection.find(query).one[T]
  }

  def update(id: String, document: T)(implicit writer: BSONDocumentWriter[T]): Future[Try[Int]] = {
    Logger.debug(s"Updating document: [collection=$collectionName, id=$id, document=$document]")
    tryIt(collection.update(DBQueryBuilder.id(id), document))
  }

  def update(id: BSONObjectID, document: T)(implicit writer: BSONDocumentWriter[T]): Future[Try[Int]] = {
    Logger.debug(s"Updating document: [collection=$collectionName, id=$id, document=$document]")
    tryIt(collection.update(DBQueryBuilder.id(id), document))
  }

  def update(id: String, query: BSONDocument): Future[Try[Int]] = {
    Logger.debug(s"Updating by query: [collection=$collectionName, id=$id, query=$query]")
    tryIt(collection.update(DBQueryBuilder.id(id), query))
  }

  def push[S](id: String, field: String, data: S)(implicit writer: BSONDocumentWriter[S]): Future[Try[Int]] = {
    Logger.debug(s"Pushing to document: [collection=$collectionName, id=$id, field=$field data=$data]")
    tryIt(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.push(field, data)))
  }

  def pull[S](id: String, field: String, query: S)(implicit writer: BSONDocumentWriter[S]): Future[Try[Int]] = {
    Logger.debug(s"Pulling from document: [collection=$collectionName, id=$id, field=$field query=$query]")
    tryIt(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.pull(field, query)))
  }

  def unset(id: String, field: String): Future[Try[Int]] = {
    Logger.debug(s"Unsetting from document: [collection=$collectionName, id=$id, field=$field]")
    tryIt(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.unset(field)))
  }

  def remove(filter: Option[String], params: Map[String, String])(implicit filterSet:FilterSet[T], attrMap:AttributeMap[T], tag : TypeTag[T] ): Future[Try[Int]] = {
    val queryDoc = buildQueryDocument(filter, params, filterSet, attrMap)
    if (queryDoc.isEmpty)
      Future {
        Failure(new IllegalArgumentException(Messages("noArgumentsProvided")))
      }
    else
      remove(queryDoc)
  }

  def remove(id: String): Future[Try[Int]] = remove(DBQueryBuilder.id(id))

  def remove(id: BSONObjectID): Future[Try[Int]] = {
    Logger.debug(s"Removing document: [collection=$collectionName, id=$id]")
    tryIt(collection.remove(DBQueryBuilder.id(id)))
  }

  def remove(query: BSONDocument, firstMatchOnly: Boolean = false): Future[Try[Int]] = {
    Logger.debug(s"Removing document(s): [collection=$collectionName, firstMatchOnly=$firstMatchOnly, query=$query]")
    tryIt(collection.remove(query, firstMatchOnly = firstMatchOnly))
  }
}
