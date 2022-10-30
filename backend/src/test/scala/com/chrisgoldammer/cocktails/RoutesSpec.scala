package com.chrisgoldammer.cocktails

import com.chrisgoldammer.cocktails.data.*
import com.chrisgoldammer.cocktails.data.types.*
import com.chrisgoldammer.cocktails.cryptocore.*
import cats.effect.IO
import junit.framework.TestSuite
import org.http4s.*
import org.http4s.implicits.*
import munit.CatsEffectSuite
import org.http4s.headers.*
import org.typelevel.ci.*
import cats.data.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import cats.implicits.*
import doobie.util.ExecutionContexts
import cats.effect.unsafe.implicits.global
import java.util.concurrent.Executors
import scala.util.Random
import org.http4s.client.{Client, JavaNetClientBuilder}
import sun.net.www.http.HttpClient

import munit.FunSuite

def resetDB(dbSetup: DBSetup) = {
  val dt = DataTools(dbSetup)
  val p = dropTables >> createTables
  p.transact(dt.xa).unsafeRunSync()
}

def getRandom(): String = Random.alphanumeric.take(20).mkString("")


def getAppForTesting(): Http4sApp = {
  val dbSetup = Settings.TestLocal.getSetup()
  return jsonApp(AppParams(dbSetup, AuthBackend.Doobie))
}

class IngredientsSpec extends CatsEffectSuite:

  val dbSetup = Settings.TestLocal.getSetup()

  val blockingPool = Executors.newFixedThreadPool(5)
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

//  test("A user cannot login without registering first") {
//    assert(withoutRegisterALoginDoesNotReturnAToken())
//  }

  test("A user can login after registering first") {
    assert(afterRegisterALoginReturnsAToken())
  }

  test("After registering, a user credentials stay valid when app is reloaded"){
    assert(newUserPersistsAfterAppReload())
  }

  test("Resetting DB prevents a user from logging in") {
    assert(resettingDBPreventsLogin())
  }

  test("After registering, we can send get user data from just the token") {
    assert(afterRegisteringOneCanSendTokenRequest())
  }

  def getRandomUser(): BasicCredentials = BasicCredentials(getRandom(), getRandom())

  def tokenUserResponse(token: String, app: Http4sApp): AuthUser = {
    val authHeader = Authorization(Credentials.Token(AuthScheme.Bearer, token.stripPrefix("\"").stripSuffix("\"").replace("Bearer ", "")))
    var getUserRequest: Request[IO] = Request[IO](Method.GET, uri"/get_user", headers = Headers(authHeader))
    val requestIO = app.run(getUserRequest)
    return requestIO.flatMap(_.as[AuthUser]).unsafeRunSync()
  }

  def loginReturnsToken(bc: BasicCredentials, app: Http4sApp): Boolean = {
    val authHeader = Authorization(bc)
    var loginRequest: Request[IO] = Request[IO](Method.GET, uri"/login", headers = Headers(authHeader))
    val requestIO = app.run(loginRequest)
    val token = requestIO.flatMap(_.as[String]).unsafeRunSync()
    println("Token" + token)
    return token.startsWith(""""Bearer """)
  }

  def withoutRegisterALoginDoesNotReturnAToken(): Boolean = {
    resetDB(dbSetup)
    val app = getAppForTesting()
    val user = getRandomUser()
    return !loginReturnsToken(user, app)
  }

  def afterRegisterALoginReturnsAToken(): Boolean = {
    println("RUNNING")
    resetDB(dbSetup)
    val app = getAppForTesting()
    val user = getRandomUser()
    println("User: " + user.toString)
    val authHeader = Authorization(user)
    val registerRequest: Request[IO] = Request[IO](Method.POST, uri"/register", headers = Headers(authHeader))
    app.run(registerRequest).unsafeRunSync()
    return loginReturnsToken(user, app)
  }

  def afterRegisteringOneCanSendTokenRequest(): Boolean = {
    resetDB(dbSetup)
    val app = getAppForTesting()

    val user = getRandomUser()
    val authHeader = Authorization(user)
    val registerRequest: Request[IO] = Request[IO](Method.POST, uri"/register", headers = Headers(authHeader))
    val registerIO = app.run(registerRequest)
    try {
      val token = registerIO.flatMap(_.as[String]).unsafeRunSync()
      val optionUser = tokenUserResponse(token, app)
      return optionUser.name == user.username
    } catch {
      case e: org.http4s.MalformedMessageBodyFailure => {
        return false
      }
    }
  }

  def newUserPersistsAfterAppReload(): Boolean = {
    resetDB(dbSetup)
    val app = getAppForTesting()

    val user = getRandomUser()

    val authHeader = Authorization(user)
    val registerRequest: Request[IO] = Request[IO](Method.POST, uri"/register", headers = Headers(authHeader))
    app.run(registerRequest).unsafeRunSync()

    val app2 = getAppForTesting()
    return loginReturnsToken(user, app2)
  }

  def resettingDBPreventsLogin(): Boolean = {
    resetDB(dbSetup)
    val app = getAppForTesting()
    val user = getRandomUser()

    val authHeader = Authorization(user)
    val registerRequest: Request[IO] = Request[IO](Method.POST, uri"/register", headers = Headers(authHeader))
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