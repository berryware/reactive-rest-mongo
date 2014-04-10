package controllers

import org.joda.time.DateTime
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID
import models.User
import scala.Some


/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class UsersSpec extends Specification {

  def bsonObjectId = BSONObjectID.generate.stringify

  "User APIs" should {

    def makeUser = User(
      bsonObjectId,
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

    "not find a user that has not been created" in new WithApplication {
      val user = route(FakeRequest(GET, "/users/" + bsonObjectId)).get

      status(user) must equalTo(NOT_FOUND)
      contentType(user) must beNone
      contentAsBytes(user).length must equalTo(0)
    }

    "not delete a user that does not exist" in new WithApplication {
      val user = route(FakeRequest(DELETE, "/users/" + bsonObjectId)).get

      status(user) must equalTo(NOT_FOUND)
      contentType(user) must beNone
      contentAsBytes(user).length must equalTo(0)
    }

    "not update a user that does not exist" in new WithApplication {
      val user = route(FakeRequest(PUT, "/users").withJsonBody(Json.toJson(makeUser))).get

      status(user) must equalTo(NOT_FOUND)
      contentType(user) must beNone
      contentAsBytes(user).length must equalTo(0)
    }

    "create and delete a user that does not exist" in new WithApplication {
      val fakeUser = makeUser

      val createuser = route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser))).get
      status(createuser) must equalTo(CREATED)
      contentType(createuser) must beNone
      contentAsBytes(createuser).length must equalTo(0)

      val deleteuser = route(FakeRequest(DELETE, "/users/" + fakeUser.id)).get
      status(deleteuser) must equalTo(OK)
      contentType(deleteuser) must beNone
      contentAsBytes(deleteuser).length must equalTo(0)
    }

    "not create a user that already exists" in new WithApplication {
      val fakeUser = makeUser

      val createuser = route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser))).get
      status(createuser) must equalTo(CREATED)
      contentType(createuser) must beNone
      contentAsBytes(createuser).length must equalTo(0)

      val user = route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser))).get
      status(user) must equalTo(BAD_REQUEST)
      contentType(user) must beSome("text/plain")

      val deleteuser = route(FakeRequest(DELETE, "/users/" + fakeUser.id)).get
      status(deleteuser) must equalTo(OK)
      contentType(deleteuser) must beNone
      contentAsBytes(deleteuser).length must equalTo(0)
    }

    "get a user that already exists" in new WithApplication {
      val fakeUser = makeUser

      val createuser = route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser))).get
      status(createuser) must equalTo(CREATED)
      contentType(createuser) must beNone
      contentAsBytes(createuser).length must equalTo(0)

      val getuser = route(FakeRequest(GET, "/users/" + fakeUser.id)).get
      status(getuser) must equalTo(OK)
      contentType(getuser) must beSome.which(_ == "application/json")
      contentAsString(getuser) must contain("\"fullName\":\"Fake User 1\"")

      val deleteuser = route(FakeRequest(DELETE, "/users/" + fakeUser.id)).get
      status(deleteuser) must equalTo(OK)
      contentType(deleteuser) must beNone
      contentAsBytes(deleteuser).length must equalTo(0)
    }

    "update a user that already exists" in new WithApplication {
      val fakeUser = makeUser
      val fakeUser2 = makeUser2(fakeUser.id)

      val createuser = route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser))).get
      status(createuser) must equalTo(CREATED)
      contentType(createuser) must beNone
      contentAsBytes(createuser).length must equalTo(0)

      val updateuser = route(FakeRequest(PUT, "/users").withJsonBody(Json.toJson(fakeUser2))).get
      status(updateuser) must equalTo(ACCEPTED)
      contentType(updateuser) must beNone
      contentAsBytes(updateuser).length must equalTo(0)

      val deleteuser = route(FakeRequest(DELETE, "/users/" + fakeUser2.id)).get
      status(deleteuser) must equalTo(OK)
      contentType(deleteuser) must beNone
      contentAsBytes(deleteuser).length must equalTo(0)
    }

    "A list of users be queryable by an integer attribute of a user" in new WithApplication {
      val fakeUser = makeUser

      val createuser = route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser))).get
      status(createuser) must equalTo(CREATED)
      contentType(createuser) must beNone
      contentAsBytes(createuser).length must equalTo(0)

      val users = route(FakeRequest(GET, "/users?age=18")).get
      status(users) must equalTo(OK)
      contentType(users) must beSome.which(_ == "application/json")
      contentAsString(users) must contain("\"fullName\":\"Fake User 1\"")

      val deleteuser = route(FakeRequest(DELETE, "/users/" + fakeUser.id)).get
      status(deleteuser) must equalTo(OK)
      contentType(deleteuser) must beNone
      contentAsBytes(deleteuser).length must equalTo(0)

    }

    "A list of users be queryable by a DateTime attribute of a user" in new WithApplication {
      val fakeUser = makeUser

      val createuser = route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser))).get
      status(createuser) must equalTo(CREATED)
      contentType(createuser) must beNone
      contentAsBytes(createuser).length must equalTo(0)

      val users = route(FakeRequest(GET, "/users?created=" + fakeUser.created.get.getMillis)).get
      status(users) must equalTo(OK)
      contentType(users) must beSome.which(_ == "application/json")
      contentAsString(users) must contain("\"fullName\":\"Fake User 1\"")

      val deleteuser = route(FakeRequest(DELETE, "/users/" + fakeUser.id)).get
      status(deleteuser) must equalTo(OK)
      contentType(deleteuser) must beNone
      contentAsBytes(deleteuser).length must equalTo(0)
    }

    "A list of users should not be found with one good match and one bad match " in new WithApplication {
      val fakeUser = makeUser

      val createuser = route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser))).get
      status(createuser) must equalTo(CREATED)
      contentType(createuser) must beNone
      contentAsBytes(createuser).length must equalTo(0)

      val users = route(FakeRequest(GET, "/users?age=17&created=" + fakeUser.created.get.getMillis)).get
      status(users) must equalTo(OK)
      contentType(users) must beSome.which(_ == "application/json")
      contentAsString(users) must not contain ("\"fullName\":\"Fake User 1\"")

      val deleteuser = route(FakeRequest(DELETE, "/users/" + fakeUser.id)).get
      status(deleteuser) must equalTo(OK)
      contentType(deleteuser) must beNone
      contentAsBytes(deleteuser).length must equalTo(0)
    }

    "A list of users be queryable by an Int and a DateTime attribute of a user" in new WithApplication {
      val fakeUser = makeUser

      val createuser = route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser))).get
      status(createuser) must equalTo(CREATED)
      contentType(createuser) must beNone
      contentAsBytes(createuser).length must equalTo(0)

      val users = route(FakeRequest(GET, "/users?age=18&created=" + fakeUser.created.get.getMillis)).get
      status(users) must equalTo(OK)
      contentType(users) must beSome.which(_ == "application/json")
      contentAsString(users) must contain("\"fullName\":\"Fake User 1\"")

      val deleteuser = route(FakeRequest(DELETE, "/users/" + fakeUser.id)).get
      status(deleteuser) must equalTo(OK)
      contentType(deleteuser) must beNone
      contentAsBytes(deleteuser).length must equalTo(0)
    }

    "A user should be queryable by a range of Ints" in new WithApplication {
      val fakeUser = makeUser

      val createuser = route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser))).get
      status(createuser) must equalTo(CREATED)
      contentType(createuser) must beNone
      contentAsBytes(createuser).length must equalTo(0)

      // test full range
      val users1 = route(FakeRequest(GET, "/users?age=(17,19)")).get
      status(users1) must equalTo(OK)
      contentType(users1) must beSome.which(_ == "application/json")
      contentAsString(users1) must contain("\"age\":18")

      // test partial range - enable once embedded mongo is in place
      val users2 = route(FakeRequest(GET, "/users?age=(17,)")).get
      status(users2) must equalTo(OK)
      contentType(users2) must beSome.which(_ == "application/json")
      contentAsString(users2) must contain("\"age\":18")

      // test partial range
      val users3 = route(FakeRequest(GET, "/users?age=(,19)")).get
      status(users3) must equalTo(OK)
      contentType(users3) must beSome.which(_ == "application/json")
      contentAsString(users3) must contain("\"age\":18")

      // test partial range - enable once embedded mongo is in place
      val users4 = route(FakeRequest(GET, "/users?age=(18,)")).get
      status(users4) must equalTo(OK)
      contentType(users4) must beSome.which(_ == "application/json")
      contentAsString(users4) must contain("\"age\":18")

      // test partial range
      val users5 = route(FakeRequest(GET, "/users?age=(,18)")).get
      status(users5) must equalTo(OK)
      contentType(users5) must beSome.which(_ == "application/json")
      contentAsString(users5) must contain("\"age\":18")

      // test partial range - enable once embedded mongo is in place
      val users6 = route(FakeRequest(GET, "/users?age=(19,)")).get
      status(users6) must equalTo(OK)
      contentType(users6) must beSome.which(_ == "application/json")
      contentAsString(users6) must not contain("\"age\":18")

      // test partial range
      val users7 = route(FakeRequest(GET, "/users?age=(,17)")).get
      status(users7) must equalTo(OK)
      contentType(users7) must beSome.which(_ == "application/json")
      contentAsString(users7) must not contain ("\"age\":18")

      val deleteuser = route(FakeRequest(DELETE, "/users/" + fakeUser.id)).get
      status(deleteuser) must equalTo(OK)
      contentType(deleteuser) must beNone
      contentAsBytes(deleteuser).length must equalTo(0)
    }

    "A list of users should return the created users" in new WithApplication {
      val fakeUser1 = makeUser
      val fakeUser2 = makeUser2(bsonObjectId)

      route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser1))).get
      route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser2))).get
      val users = route(FakeRequest(GET, "/users?q=Fake")).get
      status(users) must equalTo(OK)
      contentType(users) must beSome.which(_ == "application/json")
      contentAsString(users) must contain("\"fullName\":\"Fake User 1\"")
      contentAsString(users) must contain("\"fullName\":\"Fake User 2\"")

      val deleteuser = route(FakeRequest(DELETE, "/users/" + fakeUser1.id)).get
      status(deleteuser) must equalTo(OK)
      contentType(deleteuser) must beNone
      contentAsBytes(deleteuser).length must equalTo(0)

      val deleteuser2 = route(FakeRequest(DELETE, "/users/" + fakeUser2.id)).get
      status(deleteuser2) must equalTo(OK)
      contentType(deleteuser2) must beNone
      contentAsBytes(deleteuser2).length must equalTo(0)
    }

    "A user should be queryable by an array of Ints" in new WithApplication {
      val fakeUser = makeUser

      val createuser = route(FakeRequest(POST, "/users").withJsonBody(Json.toJson(fakeUser))).get
      status(createuser) must equalTo(CREATED)
      contentType(createuser) must beNone
      contentAsBytes(createuser).length must equalTo(0)

      // test in middle
      val users1 = route(FakeRequest(GET, "/users?age=[17,18,19]")).get
      status(users1) must equalTo(OK)
      contentType(users1) must beSome.which(_ == "application/json")
      contentAsString(users1) must contain("\"age\":18")

      // test alone
      val users3 = route(FakeRequest(GET, "/users?age=[18]")).get
      status(users3) must equalTo(OK)
      contentType(users3) must beSome.which(_ == "application/json")
      contentAsString(users3) must contain("\"age\":18")

      // test on end
      val users4 = route(FakeRequest(GET, "/users?age=[17,18]")).get
      status(users4) must equalTo(OK)
      contentType(users4) must beSome.which(_ == "application/json")
      contentAsString(users4) must contain("\"age\":18")

      // test on other end
      val users5 = route(FakeRequest(GET, "/users?age=[18,19]")).get
      status(users5) must equalTo(OK)
      contentType(users5) must beSome.which(_ == "application/json")
      contentAsString(users5) must contain("\"age\":18")


      // test not in range
      val users7 = route(FakeRequest(GET, "/users?age=[17,19,20]")).get
      status(users7) must equalTo(OK)
      contentType(users7) must beSome.which(_ == "application/json")
      contentAsString(users7) must not contain ("\"age\":18")

      val deleteuser = route(FakeRequest(DELETE, "/users/" + fakeUser.id)).get
      status(deleteuser) must equalTo(OK)
      contentType(deleteuser) must beNone
      contentAsBytes(deleteuser).length must equalTo(0)
    }
  }
}
