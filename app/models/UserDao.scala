package models

import models.dao.IdentifiableDAO
import scaldi.Injector

/**
  * Created by dberry on 17/8/16.
  */
class UserDao(implicit val injector: Injector) extends IdentifiableDAO[User] {
  val collectionName = "myusers"
}
