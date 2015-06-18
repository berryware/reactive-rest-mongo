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

import play.api.cache.Cache
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Try, Success, Failure}
import play.api.Logger
import reactivemongo.core.commands.Count
import models._
import play.api.i18n.Messages
import scala.Some
import reactivemongo.api.QueryOpts
import reactivemongo.api.collections.default.BSONCollection
import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.Type
import org.joda.time.DateTime
import reactivemongo.bson.BSONBoolean
import reactivemongo.bson.BSONDouble
import reactivemongo.bson.BSONDateTime
import scala.util.Failure
import scala.Some
import reactivemongo.api.QueryOpts
import reactivemongo.bson.BSONLong
import reactivemongo.bson.BSONInteger
import reactivemongo.bson.BSONRegex
import reactivemongo.api.collections.default.BSONCollection
import play.api.libs.iteratee.Enumerator
import play.api.cache.Cache

/**
 * Created by dberry on 13/3/14.
 */
trait MongoDao[T] extends DaoHelper {

  val collectionName: String

  import play.api.Play.current

  def collection = ReactiveMongoPlugin.db.collection[BSONCollection](collectionName)

  def insert(docList: List[T])(implicit writer: BSONDocumentWriter[T]): Future[Try[Int]] = {
    Logger.debug(s"Inserting document: [collection=$collectionName, data=$docList]")
    val enumerator = Enumerator.enumerate(docList.map( t => writer.write(t)))
    collection.bulkInsert(enumerator).map{ Success(_)}
  }

  def insert(document: T)(implicit writer: BSONDocumentWriter[T]): Future[Try[Int]] = {
    Logger.debug(s"Inserting document: [collection=$collectionName, data=$document]")
    tryIt(collection.insert(document))
  }

  def findById(id: Option[String])(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = findOne(DBQueryBuilder.id(id))

  def findById(id: BSONObjectID)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = findOne(DBQueryBuilder.id(id))

  def findAll(query: BSONDocument = BSONDocument.empty, sort: BSONDocument = BSONDocument.empty)(implicit reader: BSONDocumentReader[T]): Future[List[T]] = {
    Logger.debug(s"Finding documents: [collection=$collectionName, query=$query]")
    collection.find(query).sort(sort).cursor[T].collect[List]()
  }

  def findAll(filter: Option[String], orderby: Option[String], params: Map[String, String])(implicit reader: BSONDocumentReader[T], daoData:DaoData[T], tag : TypeTag[T] ): Future[List[T]] = {
    // Execute the query
    findAll(buildQueryDocument(filter, params, daoData), buildSortDocument(orderby, daoData.attributeMap))
  }

  def find(filter: Option[String], orderby: Option[String], page: Int, ipp: Int, params: Map[String, String])(implicit reader: BSONDocumentReader[T], daoData:DaoData[T], tag : TypeTag[T] ): Future[(List[T], Int)] = {
  // Execute the query
    find(buildQueryDocument(filter, params, daoData), buildSortDocument(orderby, daoData.attributeMap), page, ipp)
  }

  private def buildSortDocument(orderby: Option[String], attributeMap:Map[String,String]) = orderby match {
    case None => Logger.debug("orderby = None"); BSONDocument.empty
    case Some(sorts) => Logger.debug("orderby = "+sorts);
      sorts.split(",").foldLeft(BSONDocument.empty) {
        (doc, s) =>
          s.split(" ").toList match {
            case a :: Nil => doc ++ (attributeMap.getOrElse(a, a) -> 1)
            case a :: b :: Nil => doc ++ (attributeMap.getOrElse(a, a) -> b.toInt)
            case _ => doc ++ ()
          }
      }
  }

  private def buildQueryDocument(filter: Option[String], params: Map[String, String], daoData:DaoData[T])(implicit tag : TypeTag[T] ) = {
    val leftDocument = filter match {
              case None => Logger.debug("q = None"); BSONDocument.empty
              case Some(s) => Logger.debug("q = "+s)
                BSONDocument( "$or" -> daoData.filterSet.foldLeft(BSONArray.empty) { (arr, attr) => arr ++  BSONDocument(attr -> BSONRegex(".*" + s + ".*", "i")) })
            }
    val rightDocument = buildAttributeDocument(params, daoData.attributeMap)
    if (!leftDocument.isEmpty && !rightDocument.isEmpty)
      BSONDocument("$and" -> (BSONArray.empty ++ leftDocument ++ rightDocument))
    else if (!leftDocument.isEmpty)
      leftDocument
    else if (!rightDocument.isEmpty)
      rightDocument
    else
      BSONDocument.empty

  }

  private def buildAttributeDocument(params: Map[String, String], attributeMap:Map[String,String] )(implicit tag : TypeTag[T] ) = {
    params.keys.foldLeft(BSONDocument.empty) {
      (doc,key) => doc ++ processAttribute(key, params.get(key).get, attributeMap.getOrElse(key, key))
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
    // TODO: name has . notation to access members and submembers of the class, need to respect that
    tag.tpe.member(TermName(name)) match {
      case NoSymbol => BSONString(sval)
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
          case _ => if (sval.contains("*")) BSONRegex(sval.replaceAll("\\*", ".*"),"i") else BSONString(sval)  // default is a String
        }
    }
  }

  def find(query: BSONDocument = BSONDocument.empty, sort: BSONDocument = BSONDocument.empty, page: Int, ipp: Int)(implicit reader: BSONDocumentReader[T]): Future[(List[T], Int)] = {
    val queryString = BSONDocument.pretty(query)
    val sortString = BSONDocument.pretty(sort)
    Logger.debug(s"Finding documents with paging: [collection=$collectionName, query=$queryString] sort=$sortString")
    for {
      docs <- collection.find(query).sort(sort).options(QueryOpts((page-1)*ipp, ipp)).cursor[T].collect[List](ipp)
    } yield (docs, getTotalPages(query,ipp))
  }

  def enumerate(query: BSONDocument = BSONDocument.empty, sort: BSONDocument = BSONDocument.empty)(implicit reader: BSONDocumentReader[T]) = {
    val queryString = BSONDocument.pretty(query)
    val sortString = BSONDocument.pretty(sort)
    Logger.debug(s"Enumerating documents: [collection=$collectionName, query=$queryString] sort=$sortString")
    collection.find(query).sort(sort).cursor[T].enumerate()
  }

  private def getTotalPages(query:BSONDocument, ipp:Int) = {
    val key = collectionName + ":" + BSONDocument.pretty(query)
    Logger.debug("Query key = "+key)
    val totaldocs = Cache.getOrElse[Int](key) {
      val total = Await.result(ReactiveMongoPlugin.db.command(Count(collectionName, Some(query))), Duration.Inf)
      Logger.debug(s"Caching query total docs $total")
      Cache.set(key, total, 60)
      total
    }
    val totalpages = (totaldocs/ipp)+1
    Logger.debug(s"Total docs $totaldocs, Total pages $totalpages, Items per page, $ipp")
    totalpages
  }

  def findOne(paramMap:Map[String, String])(implicit reader: BSONDocumentReader[T], daoData:DaoData[T], tag : TypeTag[T] ): Future[Option[T]] = {
    Logger.debug(s"Finding one: [collection=$collectionName, paramMap=$paramMap]")
    findOne(buildAttributeDocument(paramMap, daoData.attributeMap))
  }

  def findOne(query: BSONDocument = BSONDocument.empty)(implicit reader: BSONDocumentReader[T]): Future[Option[T]] = {
    val queryString = BSONDocument.pretty(query)
    Logger.debug(s"Finding one: [collection=$collectionName, query=$queryString]")
    collection.find(query).one[T]
  }

  def update(id: Option[String], document: T)(implicit writer: BSONDocumentWriter[T]): Future[Try[Int]] = {
    Logger.debug(s"Updating document: [collection=$collectionName, id=$id, document=$document]")
    tryIt(collection.update(DBQueryBuilder.id(id), document))
  }

  def update(id: BSONObjectID, document: T)(implicit writer: BSONDocumentWriter[T]): Future[Try[Int]] = {
    Logger.debug(s"Updating document: [collection=$collectionName, id=$id, document=$document]")
    tryIt(collection.update(DBQueryBuilder.id(id), document))
  }

  def update(id: Option[String], query: BSONDocument): Future[Try[Int]] = {
    Logger.debug(s"Updating by query: [collection=$collectionName, id=$id, query=$query]")
    tryIt(collection.update(DBQueryBuilder.id(id), query))
  }

  def push[S](id: Option[String], field: String, data: S)(implicit writer: BSONDocumentWriter[S]): Future[Try[Int]] = {
    Logger.debug(s"Pushing to document: [collection=$collectionName, id=$id, field=$field data=$data]")
    tryIt(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.push(field, data)))
  }

  def pull[S](id: Option[String], field: String, query: S)(implicit writer: BSONDocumentWriter[S]): Future[Try[Int]] = {
    Logger.debug(s"Pulling from document: [collection=$collectionName, id=$id, field=$field query=$query]")
    tryIt(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.pull(field, query)))
  }

  def unset(id: Option[String], field: String): Future[Try[Int]] = {
    Logger.debug(s"Unsetting from document: [collection=$collectionName, id=$id, field=$field]")
    tryIt(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.unset(field)))
  }

  def remove(filter: Option[String], params: Map[String, String])(implicit daoData:DaoData[T], tag : TypeTag[T] ): Future[Try[Int]] = {
    val queryDoc = buildQueryDocument(filter, params, daoData)
    if (queryDoc.isEmpty)
      Future {
        Failure(new IllegalArgumentException(Messages("noArgumentsProvided")))
      }
    else
      remove(queryDoc)
  }

  def remove(id: Option[String]): Future[Try[Int]] = remove(DBQueryBuilder.id(id))

  def remove(id: BSONObjectID): Future[Try[Int]] = {
    Logger.debug(s"Removing document: [collection=$collectionName, id=$id]")
    tryIt(collection.remove(DBQueryBuilder.id(id)))
  }

  def remove(query: BSONDocument, firstMatchOnly: Boolean = false): Future[Try[Int]] = {
    Logger.debug(s"Removing document(s): [collection=$collectionName, firstMatchOnly=$firstMatchOnly, query=${BSONDocument.pretty(query)}]")
    tryIt(collection.remove(query, firstMatchOnly = firstMatchOnly))
  }

  def removeAll(): Future[Try[Int]] = remove(BSONDocument())

}
