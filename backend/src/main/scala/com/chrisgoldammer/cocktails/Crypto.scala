package com.chrisgoldammer.cocktails

import cats.*
import cats.effect.*
import cats.implicits.*
import cats.data.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.server.*

import scala.io.Codec
import scala.util.Random
import org.http4s.headers.Cookie
import org.http4s.syntax.header.*
import org.http4s.headers.Authorization

import javax.crypto.{Cipher, Mac}
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import org.apache.commons.codec.binary.Hex

import java.time.*
import cats.data.OptionT
import cats.Applicative.*

import scala.collection.mutable
import tsec.passwordhashers.jca.BCrypt.syncPasswordHasher
import org.http4s.headers.Authorization
import org.http4s.BasicCredentials
import tsec.passwordhashers.jca.*

case class PrivateKey(key: Array[Byte])

case class CryptoBits(key: PrivateKey) {

  def validatePassword(signature: String, nonce: String, raw: String): Boolean = constantTimeEquals(signature, sign(nonce + "-" + raw))

  def sign(message: String): String = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(key.key, "HmacSHA1"))
    Hex.encodeHexString(mac.doFinal(message.getBytes("utf-8")))
  }

  def signToken(token: String, nonce: String): String = {
    val joined = nonce + "-" + token
    sign(joined) + "-" + joined
  }

  def validateSignedToken(token: String): Option[String] = {
    token.split("-", 3) match {
      case Array(signature, nonce, raw) => {
        val signed = sign(nonce + "-" + raw)
        if (constantTimeEquals(signature, signed)) Some(raw) else None
      }
      case _ => None
    }
  }

  def constantTimeEquals(a: String, b: String): Boolean = {
    var equal = 0
    for (i <- 0 until (a.length min b.length)) {
      equal |= a(i) ^ b(i)
    }
    if (a.length != b.length) {
      false
    } else {
      equal == 0
    }
  }
}



case class AuthUser(id: String, name: String)
val user = AuthUser(id = "fff", name = "TestUser")



case class CreatedUserData(id: String, name: String, hash: String) {
}

def toAuthUser(that: CreatedUserData): AuthUser = AuthUser(that.id, that.name)

val createdUser = CreatedUserData(user.id, user.name, "$s0$e0801$jSIPEy4Ow8LouWdRNmEydA==$4M2oXnkqIPtT8VcU1LU+kDdgwbV893W9UvPi4oWH/A4=")

val password = "helloworld"
val userCreationData = BasicCredentials(user.name, password)

val authUser: Kleisli[OptionT[IO, *], Request[IO], AuthUser] =
  Kleisli(_ => OptionT.liftF(IO(???)))

trait BackingStore {

    def asString(): String
    def get(id: String): OptionT[IO, CreatedUserData]
    def put(elem: BasicCredentials): IO[Option[CreatedUserData]]
    def update(v: CreatedUserData): IO[CreatedUserData]
    def delete(id: String): IO[Unit]
}



// TODO: Create a way that the backingstore is a parameter on app startup
object AuthHelpers {

  def hashPassword(pass: String): IO[String] = SCrypt.hashpw[IO](pass.getBytes()).map(_.toString)

  def dummyBackingStore = new BackingStore {
    val storageMap = mutable.HashMap.empty[String, CreatedUserData]

    def getRandom(): String = Random.alphanumeric.take(20).mkString("")

    def asString(): String = storageMap.toString()

    def putIfEmpty(c: CreatedUserData): IO[Option[CreatedUserData]] = IO({
      if (storageMap.put(c.name, c).isEmpty) Some(c) else None
    })

    def getCreated(c: BasicCredentials): IO[CreatedUserData] = for {
      h <- hashPassword(c.password)
    } yield CreatedUserData(getRandom(), c.username, h)

    def put(elem: BasicCredentials): IO[Option[CreatedUserData]] = getCreated(elem).flatMap(putIfEmpty)

    def get(id: String): OptionT[IO, CreatedUserData] =
      OptionT.fromOption[IO](storageMap.get(id))

    def update(v: CreatedUserData): IO[CreatedUserData] = IO({
      storageMap.update(v.name, v)
      v
    })

    def delete(id: String): IO[Unit] =
      storageMap.remove(id) match {
        case Some(_) => IO.unit
        case None => IO.raiseError(new IllegalArgumentException)
      }
  }
}

val bStore = AuthHelpers.dummyBackingStore
val _ = bStore.put(userCreationData)


def getCredentials(request: Request[IO]): Option[BasicCredentials] = {
  val header = request.headers.get[Authorization].toList.headOption
  for {
    h <- header
    s <- Some(BasicCredentials.apply(h.credentials.toString.split(" ")(1)))
  } yield s
}


def checkUser(pass: String)(storedUser: CreatedUserData): Option[AuthUser] = {
  val isMatch = SCryptUtil.check(pass.getBytes(), storedUser.hash)
  Option.when(isMatch)(AuthUser(storedUser.id, storedUser.name))
}

val key = PrivateKey(Codec.toUTF8(Random.alphanumeric.take(20).mkString("")))
val crypto = CryptoBits(key)
val clock = java.time.Clock.systemUTC

def verifyUserExists(c: BasicCredentials): IO[Option[AuthUser]] = for {
  storedUser <- bStore.get(c.username).value
} yield storedUser.map(checkUser(c.password)).flatten.headOption

def verifyUserNameDoesNotExist(c: BasicCredentials): IO[Option[BasicCredentials]] = for {
  res <- bStore.get(c.username).value
} yield Option.when(!res.isDefined)(c)

def verifyLogin(request: Request[IO]): IO[Option[AuthUser]] = for {
  res <- getCredentials(request) match
    case None => IO.pure(None)
    case Some(c) => verifyUserExists(c)
} yield res

val logIn: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({ request =>
  verifyLogin(request: Request[IO]).flatMap(_ match {
    case None =>
      Forbidden("Bad User Credentials")
    case Some(user) => {
      val message = crypto.signToken(user.name.toString, clock.millis.toString)
      Ok("Logged in!").map(_.addCookie(ResponseCookie("authcookie", message)))
    }
  })
})

def verifyRegister(request: Request[IO]): IO[Option[BasicCredentials]] = for {

  res <- getCredentials(request) match
    case None => IO.pure({
      None
    })
    case Some(c) => {
      verifyUserNameDoesNotExist(c)
    }
} yield res



def registerUser(user: BasicCredentials): IO[Response[IO]] = for {
  _ <- bStore.put(user)
  resp <- Ok()
} yield resp

val register: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({ request =>
  verifyRegister(request: Request[IO]).flatMap(_ match {
    case None =>
      Forbidden("B")
    case Some(user) => registerUser(user)
  })
})


def getUser(id: String): IO[AuthUser] = for {
  res <- bStore.get(id).value
} yield toAuthUser(res.toList(0))

def retrieveUser: Kleisli[IO, String, AuthUser] = Kleisli(getUser)


val authorizeUserFromCookie: Kleisli[IO, Request[IO], Either[String, AuthUser]] = Kleisli({ request =>
  val message = for {
    header <- request.headers.get[Cookie]
      .toRight("Cookie parsing error")
    cookie <- header.values.toList.find(_.name == "authcookie")
      .toRight("Couldn't find the authcookie")
    token <- crypto.validateSignedToken(cookie.content)
      .toRight("Cookie invalid")
  } yield token
  message.traverse(retrieveUser.run)
})

