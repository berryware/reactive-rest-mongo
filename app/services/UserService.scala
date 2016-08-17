package services

import models.User
import models.dao.IdentifiableDAO
import scaldi.Injector

/**
  * Created by dberry on 17/8/16.
  */
class UserService(implicit val injector: Injector) extends IdentifiableDAO[User] {
  val collectionName = "myusers"
}
