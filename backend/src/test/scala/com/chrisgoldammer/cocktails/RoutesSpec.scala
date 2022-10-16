package com.chrisgoldammer.cocktails
import cats.effect.IO
import junit.framework.TestSuite
import org.http4s.*
import org.http4s.implicits.*
import munit.CatsEffectSuite
import org.http4s.headers.*
import org.typelevel.ci.*

import java.util.concurrent.Executors
import scala.util.Random
import org.http4s.client.{Client, JavaNetClientBuilder}
import sun.net.www.http.HttpClient

import munit.FunSuite

def resetDB() = {
  println("Resetting DB")
}



def getRandom(): String = Random.alphanumeric.take(20).mkString("")

def getAppForTesting(): Http4sApp = {
  var testParams = AppParams(DBSetup(), AuthBackend())
  return jsonApp(testParams)
}



class IngredientsSpec extends CatsEffectSuite:

  val blockingPool = Executors.newFixedThreadPool(5)
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  test("A random user cannot login (without registering)") {
    assert(newUserDoesNotExistWithoutRegister())
  }

  test("After registering, the user exists"){
    assert(newUserExistsAfterRegistering(), bStore.asString())
  }

  def getRandomUser(): BasicCredentials = BasicCredentials(getRandom(), getRandom())

  def doesUserExist(bc: BasicCredentials): Boolean = {
    val authHeader = Authorization(bc)
    val app = getAppForTesting()
    var loginRequest: Request[IO] = Request[IO](Method.GET, uri"/login", headers = Headers(authHeader))
    val serviceIO = app.run(loginRequest)
    val response = serviceIO.unsafeRunSync()
    val cookie = response.headers.get(CIString("Set-Cookie"))
    return cookie.nonEmpty
  }

  def newUserDoesNotExistWithoutRegister(): Boolean = {
    resetDB()
    val user = getRandomUser()
    return !doesUserExist(user)
  }

  def newUserExistsAfterRegistering(): Boolean = {
    resetDB()
    val app = getAppForTesting()

    val user = getRandomUser()
    val authHeader = Authorization(user)
    val registerRequest: Request[IO] = Request[IO](Method.GET, uri"/register", headers = Headers(authHeader))
    val registerResponse = app.run(registerRequest).unsafeRunSync()
    return doesUserExist(user)
  }


// TODO: AppParams DB works as expected
// TODO: AppParams authBackend works as expected

// TODO: If I register the same user twice, I get an error
// TODO: Send appropriate error codes if register or login go wrong
// TODO: Send nice error messages for register and login
// Todo: password hash is different each time, even with same password