package com.chrisgoldammer.cocktails

import java.util.concurrent.{ExecutorService, Executors}
import scala.util.Random
import _root_.io.circe.*
import _root_.io.circe.Decoder
import _root_.io.circe.Encoder
import _root_.io.circe.Json
import _root_.io.circe.generic.auto.*
import _root_.io.circe.generic.semiauto.*
import _root_.io.circe.generic.semiauto.deriveDecoder
import _root_.io.circe.generic.semiauto.deriveEncoder
import _root_.io.circe.syntax.*
import _root_.io.circe.syntax.EncoderOps
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxFlatMapOps
import doobie.implicits.toConnectionIOOps
import doobie.implicits.toSqlInterpolator
import doobie.util.ExecutionContexts
import org.http4s.AuthScheme
import org.http4s.BasicCredentials
import org.http4s.Credentials
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder
import org.http4s.Headers
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.circe.jsonEncoder
import org.http4s.client.Client
import org.http4s.client.JavaNetClientBuilder
import org.http4s.client.dsl.io.*
import org.http4s.dsl.io.GET
import org.http4s.dsl.io.POST
import org.http4s.headers.*
import org.http4s.implicits.uri
import org.typelevel.ci.*
import com.chrisgoldammer.cocktails.cryptocore.*
import com.chrisgoldammer.cocktails.data.*
import com.chrisgoldammer.cocktails.data.types.*
import com.chrisgoldammer.cocktails.data.queries.*
import junit.framework.TestSuite
import munit.CatsEffectSuite
import munit.FunSuite
import sun.net.www.http.HttpClient

def resetDB(dbSetup: DBSetup): Unit = {
  val dt = DataTools(dbSetup)
  val p = dropTables >> createTables
  p.transact(dt.xa).unsafeRunSync()
}

def insertDB(dbSetup: DBSetup): Unit = {
  val dt = DataTools(dbSetup)
  val p = insertFromSetupDataIO(setupDataSimple, doobieBackingStore())
  p.transact(dt.xa).unsafeRunSync()
}

def getRandom: String = Random.alphanumeric.take(20).mkString("")
def getRandomUser: BasicCredentials = BasicCredentials(getRandom, getRandom)

def getHeaderFromToken(token: String): Headers = {
  val tokenCleaned =
    token.stripPrefix("\"").stripSuffix("\"").replace("Bearer ", "")
  Headers(Authorization(Credentials.Token(AuthScheme.Bearer, tokenCleaned)))
}

def getAppForTesting: Http4sApp = {
  val dbSetup = Settings.TestLocal.getSetup
  jsonApp(AppParams(dbSetup, AuthBackend.Doobie))
}

def parseResponse(r: IO[Response[IO]]): Option[String] = {
  val res = r.unsafeRunSync()
  val body = res.as[String].unsafeRunSync()
  res.status match
    case Status.Ok => Some(body)
    case _         => None
}

def getToken(
    app: Http4sApp,
    user: BasicCredentials = getRandomUser,
    isLogin: Boolean
): Option[String] = {
  val url = if (isLogin) uri"/login" else uri"/register"
  val authHeader = Authorization(user)
  val registerRequest: Request[IO] =
    Request[IO](Method.POST, url, headers = Headers(authHeader))
  val registerIO = app.run(registerRequest)
  try {
    parseResponse(registerIO)
  } catch
      case e: org.http4s.MalformedMessageBodyFailure =>
        return None
}

def check[A](
    actual: IO[Response[IO]],
    expectedStatus: Status,
    bodyCondition: A => Boolean,
    checkBodyOnlyForOk: Boolean = true
)(implicit
    ev: EntityDecoder[IO, A]
): Boolean = {
  val actualResp = actual.unsafeRunSync()
  val statusCheck = actualResp.status == expectedStatus
  if ((expectedStatus != Status.Ok) && checkBodyOnlyForOk) {
    return statusCheck
  }
  val hasBody = actualResp.body.compile.toVector.unsafeRunSync().nonEmpty
  val responseBody = Option.when(hasBody)(actualResp.as[A].unsafeRunSync())
  val bodyCheck = responseBody match
    case None    => false
    case Some(b) => bodyCondition(b)

  statusCheck && bodyCheck
}

class DataTestAuth extends CatsEffectSuite:
  val dbSetup: DBSetup = Settings.TestLocal.getSetup
  val blockingPool: ExecutorService = Executors.newFixedThreadPool(5)
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  test("I can insert ingredient sets by uuid list") {
    resetDB(dbSetup)
    insertDB(dbSetup)
    val dt = DataTools(dbSetup)
    val app = getAppForTesting

    val user = user0
    val tokenOption = getToken(app, user, isLogin = true)
    assert(tokenOption.nonEmpty)
    val headers = tokenOption.fold(Headers.empty)(getHeaderFromToken)

    val countIng = getCount("ingredient_set_ingredients")
    val countSet = getCount("ingredient_sets")

    def getCounts: (Int, Int) = {
      val q = for {
        set <- countSet
        ing <- countIng
      } yield (set, ing)
      q.transact(dt.xa).unsafeRunSync()
    }

    val (setBefore, ingBefore) = getCounts
    val ingredientIds: List[String] = sql"""SELECT uuid FROM ingredients"""
      .query[String]
      .to[List]
      .transact(dt.xa)
      .unsafeRunSync()
    val addIngredientSetData = InsertIngredientSetData("Test", ingredientIds)
    val req =
      POST(addIngredientSetData.asJson, uri"/add_ingredient_set", headers)
    val res: Int = app.run(req).flatMap(_.as[Int]).unsafeRunSync()
    val (setAfter, ingAfter) = getCounts

    assert(setBefore + 1 == setAfter)
    assert(ingBefore + ingredientIds.size == ingAfter)
  }

class HandlerTests extends CatsEffectSuite:

  val dbSetup: DBSetup = Settings.TestLocal.getSetup
  val blockingPool: ExecutorService = Executors.newFixedThreadPool(5)
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create
  val dt: DataTools = DataTools(dbSetup)

  test("getCount works with existing table") {
    resetDB(dbSetup)
    insertDB(dbSetup)
    val counts: Int = getCount("ingredients").transact(dt.xa).unsafeRunSync()
    assert(counts > 0)
  }

class DataTests extends CatsEffectSuite:

  val dbSetup: DBSetup = Settings.TestLocal.getSetup
  val blockingPool: ExecutorService = Executors.newFixedThreadPool(5)
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  test("I can get ingredients") {
    resetDB(dbSetup)
    insertDB(dbSetup)
    val app = getAppForTesting
    val ingredientsRequest: Request[IO] =
      Request[IO](Method.GET, uri"/ingredients")
    val request = app.run(ingredientsRequest)
    def bodyCondition(a: Results[Ingredient]): Boolean = a.data.nonEmpty

    assert(check[Results[Ingredient]](request, Status.Ok, bodyCondition))
  }

  test("I can get possible recipes") {
    resetDB(dbSetup)
    insertDB(dbSetup)
    val app = getAppForTesting
    val request = app.run(Request[IO](Method.GET, uri"/ingredients"))
    val ingredients = request.flatMap(_.as[Results[Ingredient]]).unsafeRunSync()
    assert(ingredients.data.nonEmpty)

    val possibleInput: Results[String] =
      Results(data = ingredients.data.map(i => i.uuid), name = "ingredients")

    val requestPossible =
      app.run(POST(possibleInput.asJson, uri"/recipes_possible"))
    val recipesPossible =
      requestPossible.flatMap(_.as[Results[FullRecipe]]).unsafeRunSync()
    assert(recipesPossible.data.nonEmpty)

  }

  test("Ingredient sets require login") {
    resetDB(dbSetup)
    insertDB(dbSetup)
    val app = getAppForTesting
    assert(ingredientSetsCheck(app, Status.Unauthorized, Headers.empty))

    val userFromFixturesWithSet = user0
    val tokenOption = getToken(app, userFromFixturesWithSet, isLogin = true)
    val headers = tokenOption.fold(Headers.empty)(getHeaderFromToken)
    assert(ingredientSetsCheck(app, Status.Ok, headers))
  }

  test("Ingredient sets with logged-in non-owner are empty") {
    resetDB(dbSetup)
    insertDB(dbSetup)
    val app = getAppForTesting

    val user = getRandomUser
    val tokenOption = getToken(app, user, isLogin = false)
    val headers = tokenOption.fold(Headers.empty)(getHeaderFromToken)

    val ingredientsRequest: Request[IO] =
      Request[IO](Method.GET, uri"/ingredient_sets", headers = headers)
    val request = app.run(ingredientsRequest)
    def bodyCondition(a: Results[FullIngredientSet]): Boolean = a.data.isEmpty
    assert(check[Results[FullIngredientSet]](request, Status.Ok, bodyCondition))
  }

  def ingredientSetsCheck(
      app: Http4sApp,
      status: Status,
      headers: Headers
  ): Boolean = {
    val ingredientsRequest: Request[IO] =
      Request[IO](Method.GET, uri"/ingredient_sets", headers = headers)
    val request = app.run(ingredientsRequest)

    def bodyCondition(a: Results[FullIngredientSet]): Boolean = a.data.nonEmpty

    check[Results[FullIngredientSet]](request, status, bodyCondition)
  }

def getLoginRequest(isLogin: Boolean, user: BasicCredentials): Request[IO] = {
  val url = if (isLogin) uri"/login" else uri"/register"
  val authHeader = Authorization(user)
  Request[IO](Method.POST, url, headers = Headers(authHeader))
}

def getUserResponseFromToken(token: String, app: Http4sApp): Option[AuthUser] = {
  val getUserRequest: Request[IO] = Request[IO](
    Method.GET,
    uri"/get_user",
    headers = getHeaderFromToken(token)
  )
  val requestIO = app.run(getUserRequest)
  try {
    val userResponse = requestIO.flatMap(_.as[AuthUser]).unsafeRunSync()
    Some(userResponse)
  } catch {
    case e: Exception => None
  }
}


class AuthTests extends CatsEffectSuite:

  val dbSetup: DBSetup = Settings.TestLocal.getSetup
  val blockingPool: ExecutorService = Executors.newFixedThreadPool(5)
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  def registerAndThenLoginToken(user: BasicCredentials, app: Http4sApp): Option[String] = {
    val user = getRandomUser
    val registerRequest: Request[IO] = getLoginRequest(false, user)
    app.run(registerRequest).unsafeRunSync()
    getToken(app, user, true)
  }

  def hasLoginToken(app: Http4sApp, user: BasicCredentials): Boolean = getToken(app, user, isLogin = true).nonEmpty

  test("A user cannot login without registering first") {
    val app = getAppForTesting
    val user = getRandomUser
    assert(!hasLoginToken(app, user))
  }

  test("A user can login after registering first") {
    val app = getAppForTesting
    val user = getRandomUser
    val registerRequest: Request[IO] = getLoginRequest(isLogin=false, user)
    app.run(registerRequest).unsafeRunSync()
    assert(hasLoginToken(app, user))
  }

  test("User credentials stay valid when app is reloaded") {
    val app = getAppForTesting
    val user = getRandomUser
    val registerRequest: Request[IO] = getLoginRequest(false, user)
    app.run(registerRequest).unsafeRunSync()

    val app2 = getAppForTesting
    assert(hasLoginToken(app2, user))
  }

  test("After registering, we can get user data from just token") {
    val app = getAppForTesting
    val user = getRandomUser
    val tokenOption = getToken(app, user, isLogin = false)
    assert(true)
    val userNameOption = tokenOption.flatMap(token => getUserResponseFromToken(token, app)).map(_.name)
    assert(userNameOption.contains(user.username))
  }

  test("After using the fixtures, the fixture user login creates a token") {
    resetDB(dbSetup)
    insertDB(dbSetup)
    val app = getAppForTesting
    val userFromFixturesWithSet = user0
    assert(hasLoginToken(app, userFromFixturesWithSet))
  }

  test("Resetting DB prevents login") {
    resetDB(dbSetup)
    val app = getAppForTesting
    val user = getRandomUser
    val registerRequest: Request[IO] = getLoginRequest(false, user)
    app.run(registerRequest).unsafeRunSync()

    resetDB(dbSetup)
    val app2 = getAppForTesting
    assert(!hasLoginToken(app2, user))
  }

  test("Token authentication works after logging in, but not after logging out") {
    val app = getAppForTesting
    val user = getRandomUser

    val tokenOption = registerAndThenLoginToken(user, app)
    tokenOption match {
      case None => assert(false)
      case Some(token) =>
        val user: Option[AuthUser] = getUserResponseFromToken(token, app)

    }
  }




  test("Resetting DB prevents a user from logging in") {
    resetDB(dbSetup)
    val app = getAppForTesting
    val user = getRandomUser
    val registerRequest: Request[IO] = getLoginRequest(false, user)
    app.run(registerRequest).unsafeRunSync()

    resetDB(dbSetup)
    val app2 = getAppForTesting
    val tokenOption = getToken(app2, user, isLogin = true)
    assert(tokenOption.isEmpty)
  }

  test("A logout prevents cookie authentication") {
    val app = getAppForTesting
    val user = getRandomUser
    val tokenOption = registerAndThenLoginToken(user, app)
    tokenOption match {
      case None => assert(false)
      case Some(token) =>
        val authHeader = Authorization(user)
        val logoutRequest = Request[IO](Method.POST, uri"/logout", headers = getHeaderFromToken(token))
        app.run(logoutRequest).unsafeRunSync()
        println(getUserResponseFromToken(token, app))
        assert(getUserResponseFromToken(token, app).isEmpty)

    }
  }


// TODO: AppParams DB works as expected
// TODO: If I register the same user twice, I get an error
// TODO: Send appropriate error codes if register or login go wrong
// TODO: Send nice error messages for register and login
// Todo: password hash is different each time, even with same password
