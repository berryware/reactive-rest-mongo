package controllers

import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import de.flapdoodle.embed.mongo.distribution.Version
import models.User
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, MustMatchers, Suite, WordSpec}
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID
import scaldi.Injectable
import scaldi.play.ScaldiApplicationBuilder

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.test._

trait EmbeddedMongoDBPerSuite extends MongoEmbedDatabase with Injectable with BeforeAndAfterAll { this: Suite =>
  private val mongoProps: MongodProps = mongoStart(54321, Version.V3_6_5)

  override def afterAll() = {
    try super.afterAll()
    finally mongoStop(mongoProps)
  }
}

trait TestServerPerSuite extends Injectable with BeforeAndAfterAll { this: Suite =>
  val testServerPort = 19001

  implicit val testApplicationInj = new ScaldiApplicationBuilder()
    .configure(("mongodb.uri", "mongodb://localhost:54321/SBossTest"))
    .buildInj()

  implicit val testApplication = inject [Application]

  private val testServer = TestServer(testServerPort, testApplication)

  override def beforeAll(): Unit = {
    testServer.start()
    super.beforeAll()
  }

  override def afterAll() = {
    try super.afterAll()
    finally {
      testServer.stop()
    }
  }
}

class UsersSpec extends WordSpec with Injectable with MustMatchers with EmbeddedMongoDBPerSuite with TestServerPerSuite {

  private implicit val implicitPort = testServerPort

  "User APIs" must {

     def bsonObjectId = BSONObjectID.generate.stringify

    def makeUser(id : Option[String]) = User(
      id,
      "First 1",
      "Last 1",
      "Fake User 1",
      Some(18),
      Some("fake.user1@fake.com"),
      None,
      Some(DateTime.now),
      Some(DateTime.now)
    )

    def makeUser2(id: Option[String]) = User(
      id,
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
      val response = Await.result(WsTestClient.wsUrl("/users/" + bsonObjectId).get, Duration.Inf)
      response.status mustBe NOT_FOUND
      response.header(CONTENT_TYPE) mustBe None
      response.bodyAsBytes.length mustBe 0
    }

    "not delete a user that does not exist" in {
      val response = Await.result(WsTestClient.wsUrl("/users/"+bsonObjectId).delete, Duration.Inf)
      response.status mustBe NOT_FOUND
      response.header(CONTENT_TYPE) mustBe None
      response.bodyAsBytes.length mustBe 0
    }

    "not update a user that does not exist" in {
      val response = Await.result(WsTestClient.wsUrl("/users").put(Json.toJson(makeUser(Some(bsonObjectId)))), Duration.Inf)
      response.status mustBe NOT_FOUND
      response.header(CONTENT_TYPE) mustBe None
      response.bodyAsBytes.length mustBe 0
    }

    "create user that does not exist and then delete the user" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "not create a user that already exists" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]


      val user = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(makeUser(Some(id)))), Duration.Inf)
      user.status mustBe BAD_REQUEST
      user.header(CONTENT_TYPE) mustBe defined
      user.header(CONTENT_TYPE) mustBe Some("text/plain; charset=UTF-8")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "get a user that already exists" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      val getuser = Await.result(WsTestClient.wsUrl("/users/"+id).get, Duration.Inf)
      getuser.status mustBe OK
      getuser.header(CONTENT_TYPE) mustBe defined
      getuser.header(CONTENT_TYPE) mustBe Some("application/json")
      getuser.body must include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "get a user that already exists by alternate attributes" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      val getuser = Await.result(WsTestClient.wsUrl("/users/alt").withQueryStringParameters(("firstName",fakeUser.firstName),("lastName",fakeUser.lastName)).get, Duration.Inf)
      getuser.status mustBe OK
      getuser.header(CONTENT_TYPE) mustBe defined
      getuser.header(CONTENT_TYPE) mustBe Some("application/json")
      getuser.body must include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "update a user that already exists" in {
      val fakeUser1 = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser1)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      val fakeUser2 = makeUser2(Some(id))
      val updateuser = Await.result(WsTestClient.wsUrl("/users").put(Json.toJson(fakeUser2)), Duration.Inf)
      updateuser.status mustBe ACCEPTED
      updateuser.header(CONTENT_TYPE) mustBe None
      updateuser.bodyAsBytes.length mustBe 0

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+fakeUser2.id.get).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0
     }

    "return a list of users queryable by an integer attribute" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","18")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a list of users queryable by a DateTime attribute" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("created",fakeUser.created.get.getMillis.toString)).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "not return a list of users queried with one good match and one bad match " in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","17"),("created",fakeUser.created.get.getMillis.toString)).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must not include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a list of users queryable by an Int and a DateTime attribute" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","18"),("created",fakeUser.created.get.getMillis.toString)).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"fullName\":\"Fake User 1\"")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a complete range of Ints" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      // test full range
      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","(17,19)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "return a user queryable by a right-end open partial range of Ints" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

     // test partial range - enable once embedded mongo is in place
     val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","(17,)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a left-end open partial range of Ints" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      // test partial range
      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","(,19)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a right-end open partial inclusive range of Ints" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      // test partial range - enable once embedded mongo is in place
      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","(18,)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a left-end open partial inclusive range of Ints" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      // test partial range
      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","(,18)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a right-end open partial exclusive range of Ints" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      // test partial range - enable once embedded mongo is in place
      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","(19,)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must not include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a left-end open partial exclusive range of Ints" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      // test partial range
      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","(,17)")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must not include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

   }

    "return a list of users when searching text fields" in {
      val fakeUser1 = makeUser(None)
      val fakeUser2 = makeUser2(None)

      val createuser1 = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser1)), Duration.Inf)
      createuser1.status mustBe CREATED
      createuser1.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser1.bodyAsBytes.length mustBe 26
      val id1 = Json.parse(createuser1.body).as[String]

      val createuser2 = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser2)), Duration.Inf)
      createuser2.status mustBe CREATED
      createuser2.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser2.bodyAsBytes.length mustBe 26
      val id2 = Json.parse(createuser2.body).as[String]

      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("q","Fake")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"fullName\":\"Fake User 1\"")
      users.body must include ("\"fullName\":\"Fake User 2\"")

      val deleteuser1 = Await.result(WsTestClient.wsUrl("/users/"+id1).delete, Duration.Inf)
      deleteuser1.status mustBe OK
      deleteuser1.header(CONTENT_TYPE) mustBe None
      deleteuser1.bodyAsBytes.length mustBe 0

      val deleteuser2 = Await.result(WsTestClient.wsUrl("/users/"+id2).delete, Duration.Inf)
      deleteuser2.status mustBe OK
      deleteuser2.header(CONTENT_TYPE) mustBe None
      deleteuser2.bodyAsBytes.length mustBe 0
    }

    "return a user queryable by an array of Ints" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      // test in middle
      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","[17,18,19]")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by a 1-element array of Ints" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      // test alone
      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","[18]")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "return a user queryable by an array of Ints with match in last element" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      // test on end
      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","[17,18]")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }

    "return a user queryable by an array of Ints with a match in first element" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      // test on other end
      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","[18,19]")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

     }

    "not return a user  queryable by an array of Ints with no matching element" in {
      val fakeUser = makeUser(None)

      val createuser = Await.result(WsTestClient.wsUrl("/users").post(Json.toJson(fakeUser)), Duration.Inf)
      createuser.status mustBe CREATED
      createuser.header(CONTENT_TYPE) mustBe Some("application/json")
      createuser.bodyAsBytes.length mustBe 26
      val id = Json.parse(createuser.body).as[String]

      // test not in range
      val users = Await.result(WsTestClient.wsUrl("/users").withQueryStringParameters(("age","[17,19,20]")).get, Duration.Inf)
      users.status mustBe OK
      users.header(CONTENT_TYPE) mustBe defined
      users.header(CONTENT_TYPE) mustBe Some("application/json")
      users.body must not include ("\"age\":18")

      val deleteuser = Await.result(WsTestClient.wsUrl("/users/"+id).delete, Duration.Inf)
      deleteuser.status mustBe OK
      deleteuser.header(CONTENT_TYPE) mustBe None
      deleteuser.bodyAsBytes.length mustBe 0

    }
  }
}
