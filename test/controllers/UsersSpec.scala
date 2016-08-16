package controllers

import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import de.flapdoodle.embed.mongo.distribution.Version
import models.User
import org.joda.time.DateTime
import org.scalatest.{ParallelTestExecution, BeforeAndAfterAll}
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID
import scaldi.Injectable
import scaldi.play.ScaldiApplicationBuilder

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class UsersSpec extends PlaySpec with Injectable with OneServerPerSuite with BeforeAndAfterAll with MongoEmbedDatabase {

  implicit override lazy val app = new ScaldiApplicationBuilder()
    .configure(("mongodb.uri", "mongodb://localhost:12345/test"))
    .build()

  var mongoProps: MongodProps = null

  override def beforeAll {
    mongoProps = mongoStart(12345, Version.V2_7_1) // by default port = 12345 & version = Version.2.3.0
                                                   // add your own port & version parameters in mongoStart method if you need it
  }

  override def afterAll { mongoStop(mongoProps) }

  "User APIs" must {

     def bsonObjectId = BSONObjectID.generate.stringify

    def makeUser = User(
      Some(bsonObjectId),
      "First 1",
      "Last 1",
      "Fake User 1",
      Some(18),
      Some("fake.user1@fake.com"),
      None,
      Some(DateTime.now),
      Some(DateTime.now)
    )

    def makeUser2(id: String) = User(
      Some(id),
      "First 2",
      "Last 2",
      "Fake User 2",
      Some(21),
      Some("fake.user2@fake.com"),
      None,
      Some(DateTime.now),
      Some(DateTime.now)
    )

    "not find a user that has not been created" in {
      val response = Await.result(wsUrl("/users/"+bsonObjectId).get, Duration.Inf)
      response.status mustBe NOT_FOUND
      response.header(CONTENT_TYPE) mustBe None
      response.bodyAsBytes.length mustBe 0
    }

    "not delete a user that does not exist" in {
      val response = Await.result(wsUrl("/users/"+bsonObjectId).delete, Duration.Inf)
      response.status mustBe NOT_FOUND
      response.header(CONTENT_TYPE) mustBe None
      response.bodyAsBytes.length mustBe 0
    }

    "not update a user that does not exist" in {
      val response = Await.result(wsUrl("/users").put(Json.toJson(makeUser)), Duration.Inf)
      response.status mustBe NOT_FOUND
      response.header(CONTENT_TYPE) mustBe None
      response.bodyAsBytes.length mustBe 0
    }

    "create user that does not exist and then delete the user" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "not create a user that already exists" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0


      val user = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      user.status mustBe BAD_REQUEST
      user.header(CONTENT_TYPE) mustBe defined
      user.header(CONTENT_TYPE) mustBe Some("text/plain; charset=utf-8")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "get a user that already exists" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      val getuser = Await.result(wsUrl("/users/"+fakeUser.id.get).get, Duration.Inf)
      getuser.status mustBe OK
      getuser.header(CONTENT_TYPE) mustBe defined
      getuser.header(CONTENT_TYPE) mustBe Some("application/json")
      getuser.body must include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "get a user that already exists by alternate attributes" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      val getuser = Await.result(wsUrl("/users/alt").withQueryString(("firstName",fakeUser.firstName),("lastName",fakeUser.lastName)).get, Duration.Inf)
      getuser.status mustBe OK
      getuser.header(CONTENT_TYPE) mustBe defined
      getuser.header(CONTENT_TYPE) mustBe Some("application/json")
      getuser.body must include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "update a user that already exists" in {
      val fakeUser1 = makeUser
      val fakeUser2 = makeUser2(fakeUser1.id.get)

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser1)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      val updateuser = Await.result(wsUrl("/users").put(Json.toJson(fakeUser2)), Duration.Inf)
      updateuser.status mustBe ACCEPTED
      updateuser.header(CONTENT_TYPE) mustBe None
      updateuser.bodyAsBytes.length mustBe 0

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser2.id.get).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0
     }

    "return a list of users queryable by an integer attribute" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      val users = Await.result(wsUrl("/users").withQueryString(("age","18")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a list of users queryable by a DateTime attribute" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      val users = Await.result(wsUrl("/users").withQueryString(("created",fakeUser.created.get.getMillis.toString)).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "not return a list of users queried with one good match and one bad match " in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      val users = Await.result(wsUrl("/users").withQueryString(("age","17"),("created",fakeUser.created.get.getMillis.toString)).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must not include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a list of users queryable by an Int and a DateTime attribute" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      val users = Await.result(wsUrl("/users").withQueryString(("age","18"),("created",fakeUser.created.get.getMillis.toString)).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a complete range of Ints" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      // test full range
      val users = Await.result(wsUrl("/users").withQueryString(("age","(17,19)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "return a user queryable by a right-end open partial range of Ints" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

     // test partial range - enable once embedded mongo is in place
     val users = Await.result(wsUrl("/users").withQueryString(("age","(17,)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a left-end open partial range of Ints" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      // test partial range
      val users = Await.result(wsUrl("/users").withQueryString(("age","(,19)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a right-end open partial inclusive range of Ints" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      // test partial range - enable once embedded mongo is in place
      val users = Await.result(wsUrl("/users").withQueryString(("age","(18,)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a left-end open partial inclusive range of Ints" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      // test partial range
      val users = Await.result(wsUrl("/users").withQueryString(("age","(,18)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a right-end open partial exclusive range of Ints" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      // test partial range - enable once embedded mongo is in place
      val users = Await.result(wsUrl("/users").withQueryString(("age","(19,)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must not include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a left-end open partial exclusive range of Ints" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      // test partial range
      val users = Await.result(wsUrl("/users").withQueryString(("age","(,17)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must not include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

   }

    "return a list of users when searching text fields" in {
      val fakeUser1 = makeUser
      val fakeUser2 = makeUser2(bsonObjectId)

      val createuser1 = Await.result(wsUrl("/users").post(Json.toJson(fakeUser1)), Duration.Inf)
      createuser1.status mustBe CREATED
      createuser1.header(CONTENT_TYPE) mustBe None
      createuser1.bodyAsBytes.length mustBe 0

      val createuser2 = Await.result(wsUrl("/users").post(Json.toJson(fakeUser2)), Duration.Inf)
      createuser2.status mustBe CREATED
      createuser2.header(CONTENT_TYPE) mustBe None
      createuser2.bodyAsBytes.length mustBe 0

      val users = Await.result(wsUrl("/users").withQueryString(("q","Fake")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"fullName\":\"Fake User 1\"")
      users.body must include ("\"fullName\":\"Fake User 2\"")

      val deleteuser1 = Await.result(wsUrl("/users/"+fakeUser1.id.get).delete, Duration.Inf)
      deleteuser1.status mustBe OK
      deleteuser1.header(CONTENT_TYPE) mustBe None
      deleteuser1.bodyAsBytes.length mustBe 0

      val deleteuser2 = Await.result(wsUrl("/users/"+fakeUser2.id.get).delete, Duration.Inf)
      deleteuser2.status mustBe OK
      deleteuser2.header(CONTENT_TYPE) mustBe None
      deleteuser2.bodyAsBytes.length mustBe 0
    }

    "return a user queryable by an array of Ints" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      // test in middle
      val users = Await.result(wsUrl("/users").withQueryString(("age","[17,18,19]")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a 1-element array of Ints" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      // test alone
      val users = Await.result(wsUrl("/users").withQueryString(("age","[18]")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "return a user queryable by an array of Ints with match in last element" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      // test on end
      val users = Await.result(wsUrl("/users").withQueryString(("age","[17,18]")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by an array of Ints with a match in first element" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      // test on other end
      val users = Await.result(wsUrl("/users").withQueryString(("age","[18,19]")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "not return a user  queryable by an array of Ints with no matching element" in {
      val fakeUser = makeUser

      val createuser = Await.result(wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe None
      createuser.bodyAsBytes.length mustBe 0

      // test not in range
      val users = Await.result(wsUrl("/users").withQueryString(("age","[17,19,20]")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must not include ("\"age\":18")

      val deleteuser = Await.result(wsUrl("/users/"+fakeUser.id.get).delete, Duration.Inf)
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }
  }
}
