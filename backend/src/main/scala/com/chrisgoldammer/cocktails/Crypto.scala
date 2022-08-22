package com.chrisgoldammer.cocktails

import cats._, cats.effect._, cats.implicits._, cats.data._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server._

import scala.io.Codec
import scala.util.Random
import org.http4s.headers.Cookie

import org.http4s.syntax.header._
import org.http4s.headers.Authorization

import javax.crypto.{Cipher, Mac}
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import org.apache.commons.codec.binary.Hex
import java.time._
import cats.data.OptionT
import cats.Applicative.*

case class PrivateKey(key: Array[Byte])

case class CryptoBits(key: PrivateKey) {

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


case class AuthUser(id: Long, name: String)

val authUser: Kleisli[OptionT[IO, *], Request[IO], AuthUser] =
  Kleisli(_ => OptionT.liftF(IO(???)))

val middleware: AuthMiddleware[IO, AuthUser] = AuthMiddleware(authUser)

val authedRoutes: AuthedRoutes[AuthUser, IO] =
  AuthedRoutes.of {
    case GET -> Root / "welcome" as user => Ok(s"Welcome, ${user.name}")
  }

val service: HttpRoutes[IO] = middleware(authedRoutes)

val spanishRoutes: AuthedRoutes[AuthUser, IO] =
  AuthedRoutes.of {
    case GET -> Root / "hola" as user => Ok(s"Hola, ${user.name}")
  }

val frenchRoutes: HttpRoutes[IO] =
  HttpRoutes.of {
    case GET -> Root / "bonjour" => Ok(s"Bonjour")
  }

val serviceSpanish: HttpRoutes[IO] = middleware(spanishRoutes) <+> frenchRoutes

val key = PrivateKey(Codec.toUTF8(Random.alphanumeric.take(20).mkString("")))
val crypto = CryptoBits(key)
val clock = java.time.Clock.systemUTC

/*
Todo:
- Fix  `verifyLogin` so that I check against the DB`
-
*/

val user = AuthUser(id = 1, name = "TestUser")

// gotta figure out how to do the form
def verifyLogin(request: Request[IO]): IO[Either[String, AuthUser]] = for {
  x: Either[String, AuthUser] <- IO.pure(Right(user))
} yield x

def verifyLoginReal(request: Request[IO]): IO[Either[String, AuthUser]] = for {
  /*
  1. Extract username and password from the request
  2. Connect to the DB and the DB returns the AuthUser if correct
  */
  x: Either[String, AuthUser] <- IO.pure(Right(user))
} yield x

val logIn: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({ request =>
  verifyLogin(request: Request[IO]).flatMap(_ match {
    case Left(error) =>
      Forbidden(error)
    case Right(user) => {
      val message = crypto.signToken(user.id.toString, clock.millis.toString)
      Ok("Logged in!").map(_.addCookie(ResponseCookie("authcookie", message)))
    }
  })
})

def retrieveUser: Kleisli[IO, Long, AuthUser] = Kleisli(id => IO(???))
val authUserCookie: Kleisli[IO, Request[IO], Either[String, AuthUser]] = Kleisli({ request =>
  val message = for {
    header <- request.headers.get[Cookie]
      .toRight("Cookie parsing error")
    cookie <- header.values.toList.find(_.name == "authcookie")
      .toRight("Couldn't find the authcookie")
    token <- crypto.validateSignedToken(cookie.content)
      .toRight("Cookie invalid")
    message <- Either.catchOnly[NumberFormatException](token.toLong)
      .leftMap(_.toString)
  } yield message
  message.traverse(retrieveUser.run)
})

val authUserHeaders: Kleisli[IO, Request[IO], Either[String, AuthUser]] = Kleisli({ request =>
  val message = for {
    header <- request.headers.get[Authorization]
      .toRight("Couldn't find an Authorization header")
    token <- crypto.validateSignedToken(header.value)
      .toRight("Invalid token")
    message <- Either.catchOnly[NumberFormatException](token.toLong)
      .leftMap(_.toString)
  } yield message
  message.traverse(retrieveUser.run)
})

