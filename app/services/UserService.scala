package services

import models.User
import org.exaxis.smd.IdentifiableDAO
import play.modules.reactivemongo.ReactiveMongoApi
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext

/**
  * An implementation of an IdentifiableDAO. It is responsible for associating the case class to its collection in the
  * mongodb
  *
  * @param injector
  */
class UserService(implicit val injector: Injector) extends IdentifiableDAO[User] with Injectable {
  implicit val executionContext = inject [ExecutionContext]
  val reactiveMongoApi = inject[ReactiveMongoApi]
  val defaultDBFuture = reactiveMongoApi.database

  val collectionName = "myusers"
}
