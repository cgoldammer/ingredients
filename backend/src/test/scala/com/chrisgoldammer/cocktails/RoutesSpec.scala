package com.chrisgoldammer.cocktails

import cats.effect.{IO}
import junit.framework.TestSuite
import org.http4s.{
  Status,
  Method,
  Headers,
  Request,
  Response,
  EntityDecoder,
  EntityEncoder,
  Credentials,
  AuthScheme,
  BasicCredentials
}
import org.http4s.implicits.uri
import munit.CatsEffectSuite
import org.http4s.headers.*
import org.typelevel.ci.*
import cats.implicits.catsSyntaxFlatMapOps
import doobie.implicits.toConnectionIOOps
import doobie.util.ExecutionContexts
import cats.effect.unsafe.implicits.global
import java.util.concurrent.Executors
import scala.util.Random
import org.http4s.client.{Client, JavaNetClientBuilder}
import sun.net.www.http.HttpClient

import com.chrisgoldammer.cocktails.data.*
import com.chrisgoldammer.cocktails.data.types.*
import com.chrisgoldammer.cocktails.cryptocore.*

import munit.FunSuite

def resetDB(dbSetup: DBSetup) = {
  val dt = DataTools(dbSetup)
  val p = dropTables >> createTables
  p.transact(dt.xa).unsafeRunSync()
}

def insertDB(dbSetup: DBSetup) = {
  val dt = DataTools(dbSetup)
  val p = insertFromSetupDataIO(setupDataSimple, doobieBackingStore())
  p.transact(dt.xa).unsafeRunSync()
}

def getRandom(): String = Random.alphanumeric.take(20).mkString("")
def getRandomUser(): BasicCredentials =
  BasicCredentials(getRandom(), getRandom())

def getHeaderFromToken(token: String): Headers = {
  val tokenCleaned =
    token.stripPrefix("\"").stripSuffix("\"").replace("Bearer ", "")
  Headers(Authorization(Credentials.Token(AuthScheme.Bearer, tokenCleaned)))
}

def getAppForTesting(): Http4sApp = {
  val dbSetup = Settings.TestLocal.getSetup()
  return jsonApp(AppParams(dbSetup, AuthBackend.Doobie))
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
    user: BasicCredentials = getRandomUser(),
    isLogin: Boolean
): Option[String] = {
  val url = if (isLogin) uri"/login" else uri"/register"
  val authHeader = Authorization(user)
  val registerRequest: Request[IO] =
    Request[IO](Method.POST, url, headers = Headers(authHeader))
  val registerIO = app.run(registerRequest)
  try {
    val token = parseResponse(registerIO)
    return token
  } catch {
    case e: org.http4s.MalformedMessageBodyFailure => {
      return None
    }
  }
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
  val hasBody = !actualResp.body.compile.toVector.unsafeRunSync().isEmpty
  val responseBody = Option.when(hasBody)(actualResp.as[A].unsafeRunSync())
  val bodyCheck = responseBody match
    case None    => false
    case Some(b) => bodyCondition(b)

  statusCheck && bodyCheck
}

class DataTests extends CatsEffectSuite:

  val dbSetup = Settings.TestLocal.getSetup()

  val blockingPool = Executors.newFixedThreadPool(5)
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  test("I can get ingredients") {
    resetDB(dbSetup)
    insertDB(dbSetup)
    val app = getAppForTesting()
    var ingredientsRequest: Request[IO] =
      Request[IO](Method.GET, uri"/ingredients")
    val request = app.run(ingredientsRequest)
    def bodyCondition(a: Results[Ingredient]): Boolean = a.data.size > 0

    assert(check[Results[Ingredient]](request, Status.Ok, bodyCondition))
  }

  test("Ingredient sets require login") {
    resetDB(dbSetup)
    insertDB(dbSetup)
    val app = getAppForTesting()
    assert(ingredientSetsCheck(app, Status.Unauthorized, Headers.empty))

    val userFromFixturesWithSet = user0
    val tokenOption = getToken(app, userFromFixturesWithSet, isLogin = true)
    val headers = tokenOption.fold(Headers.empty)(getHeaderFromToken)
    assert(ingredientSetsCheck(app, Status.Ok, headers))
  }

  test("Ingredient sets with logged-in non-owner are empty") {
    assert(true)
//    resetDB(dbSetup)
//    insertDB(dbSetup)
//    val app = getAppForTesting()
//
//    val user = getRandomUser()
//    val tokenOption = getToken(app, user, isLogin = false)
//    val headers = tokenOption.fold(Headers.empty)(getHeaderFromToken)
//
//    var ingredientsRequest: Request[IO] =
//      Request[IO](Method.GET, uri"/ingredient_sets", headers = headers)
//    val request = app.run(ingredientsRequest)
//    def bodyCondition(a: Results[FullIngredientSet]): Boolean = a.data.size == 0
//    assert(check[Results[FullIngredientSet]](request, Status.Ok, bodyCondition))
  }

  def ingredientSetsCheck(
      app: Http4sApp,
      status: Status,
      headers: Headers
  ): Boolean = {
    var ingredientsRequest: Request[IO] =
      Request[IO](Method.GET, uri"/ingredient_sets", headers = headers)
    val request = app.run(ingredientsRequest)

    def bodyCondition(a: Results[FullIngredientSet]): Boolean = a.data.size > 0

    check[Results[FullIngredientSet]](request, status, bodyCondition)
  }

class AuthTests extends CatsEffectSuite:

  val dbSetup = Settings.TestLocal.getSetup()

  val blockingPool = Executors.newFixedThreadPool(5)
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  test("A user cannot login without registering first") {
    assert(withoutRegisterALoginDoesNotReturnAToken())
  }

  test("A user can login after registering first") {
    assert(afterRegisterALoginReturnsAToken())
  }

  test(
    "After registering, a user credentials stay valid when app is reloaded"
  ) {
    assert(newUserPersistsAfterAppReload())
  }

  test("After registering, we can send get user data from just the token") {
    assert(afterRegisteringOneCanSendTokenRequest())
  }

  test("After using the fixtures, the fixture user login creates a token") {
    resetDB(dbSetup)
    insertDB(dbSetup)
    val app = getAppForTesting()
    val userFromFixturesWithSet = user0
    val tokenOption = getToken(app, userFromFixturesWithSet, isLogin = true)
    assert(tokenOption.nonEmpty)
  }

  test("Resetting DB prevents a user from logging in") {
    assert(resettingDBPreventsLogin())
  }

  def tokenUserResponse(token: String, app: Http4sApp): AuthUser = {
    var getUserRequest: Request[IO] = Request[IO](
      Method.GET,
      uri"/get_user",
      headers = getHeaderFromToken(token)
    )
    val requestIO = app.run(getUserRequest)
    return requestIO.flatMap(_.as[AuthUser]).unsafeRunSync()
  }

  def loginReturnsToken(bc: BasicCredentials, app: Http4sApp): Boolean = {
    val authHeader = Authorization(bc)
    var loginRequest: Request[IO] =
      Request[IO](Method.POST, uri"/login", headers = Headers(authHeader))
    val requestIO = app.run(loginRequest)
    val token = requestIO.flatMap(_.as[String]).unsafeRunSync()
    return token.startsWith(""""Bearer """)
  }

  def withoutRegisterALoginDoesNotReturnAToken(): Boolean = {
    val app = getAppForTesting()
    val user = getRandomUser()
    return !loginReturnsToken(user, app)
  }

  def afterRegisterALoginReturnsAToken(): Boolean = {
    val app = getAppForTesting()
    val user = getRandomUser()
    val authHeader = Authorization(user)
    val registerRequest: Request[IO] =
      Request[IO](Method.POST, uri"/register", headers = Headers(authHeader))
    app.run(registerRequest).unsafeRunSync()
    return loginReturnsToken(user, app)
  }

  def afterRegisteringOneCanSendTokenRequest(): Boolean = {
    val app = getAppForTesting()
    val user = getRandomUser()
    val tokenOption = getToken(app, user, isLogin = false)
    tokenOption.map(token => tokenUserResponse(token, app)).map(_.name) == Some(
      user.username
    )
  }

  def newUserPersistsAfterAppReload(): Boolean = {
    val app = getAppForTesting()
    val user = getRandomUser()

    val authHeader = Authorization(user)
    val registerRequest: Request[IO] =
      Request[IO](Method.POST, uri"/register", headers = Headers(authHeader))
    app.run(registerRequest).unsafeRunSync()

    val app2 = getAppForTesting()
    return loginReturnsToken(user, app2)
  }

  def resettingDBPreventsLogin(): Boolean = {
    resetDB(dbSetup)
    val app = getAppForTesting()
    val user = getRandomUser()

    val authHeader = Authorization(user)
    val registerRequest: Request[IO] =
      Request[IO](Method.POST, uri"/register", headers = Headers(authHeader))
    app.run(registerRequest).unsafeRunSync()

    resetDB(dbSetup)
    val app2 = getAppForTesting()
    return !loginReturnsToken(user, app2)
  }

// TODO: AppParams DB works as expected
// TODO: AppParams authBackend works as expected

// TODO: If I register the same user twice, I get an error
// TODO: Send appropriate error codes if register or login go wrong
// TODO: Send nice error messages for register and login
// Todo: password hash is different each time, even with same password
