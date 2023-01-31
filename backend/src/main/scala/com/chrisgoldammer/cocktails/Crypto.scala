package com.chrisgoldammer.cocktails

import java.time.*

import scala.collection.mutable
import scala.io.Codec
import scala.util.Random

import _root_.io.circe.Json
import _root_.io.circe.generic.auto.*
import _root_.io.circe.parser.{parse => jsonParse}
import _root_.io.circe.syntax.*
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import doobie.Transactor
import doobie.implicits.toConnectionIOOps
import org.apache.commons.codec.binary.Hex
import org.http4s.BasicCredentials
import org.http4s.{Request, ContextRequest}
import org.http4s.Response
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.http4s.server.*
import org.http4s.syntax.header.*

import com.chrisgoldammer.cocktails.cryptocore.*
import com.chrisgoldammer.cocktails.data.types.*
import com.chrisgoldammer.cocktails.data.queries.*

import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import tsec.passwordhashers.jca.*
import tsec.passwordhashers.jca.BCrypt.syncPasswordHasher

def toEither[A, B](a: Option[A], b: B): Either[B, A] =
  Either.cond(a.isDefined, a.get, b)

val key = PrivateKey(Codec.toUTF8(Random.alphanumeric.take(20).mkString("")))
val crypto = CryptoBits(PrivateKey(Codec.toUTF8(secrets.cryptocorePrivatekey)))
val clock = java.time.Clock.systemUTC
val defaultTokenError = "Token not valid"

case class AuthFunctions(ab: AuthBackend, dbSetup: DBSetup) {
  private val backend: BackingStore = ab.getBackingStore
  val xa: Transactor[IO] = getTransactor(dbSetup)

  private def verifyUserExists(c: BasicCredentials): IO[Option[AuthUser]] = for {
    storedUser <- backend.get(c.username).transact(xa)
  } yield storedUser.flatMap(checkUser(c.password))

  private def verifyUserNameDoesNotExist(
                                  c: BasicCredentials
                                ): IO[Option[BasicCredentials]] = for {
    res <- backend.get(c.username).transact(xa)
  } yield Option.when(res.isEmpty)(c)

  private def verifyLogin(request: Request[IO]): IO[Option[AuthUser]] = for {
    res <- getCredentials(request) match
      case None => IO.pure(None)
      case Some(c) => verifyUserExists(c)
  } yield res

  private def registerUser(user: BasicCredentials): IO[Response[IO]] = for {
    _ <- backend.put(user).transact(xa)
    resp <- Ok(getBearer(user.username))
  } yield resp

  val register: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({ request =>
    verifyRegister(request: Request[IO]).flatMap({
      case None =>
        Forbidden(jsonParseString("Registration failed"))
      case Some(user) => registerUser(user)
    })
  })

  private def getUser(id: String): IO[Option[AuthUser]] = for {
    res <- backend.get(id).transact(xa)
  } yield res.map(toAuthUser)

  private def retrieveUser: Kleisli[IO, String, Option[AuthUser]] = Kleisli(getUser)

  private def jsonParseString(s: String): Json =
    jsonParse(raw""""$s"""").getOrElse(Json.Null)

  private def getBearer(user: String, expirationSeconds: Int = 1000): Json = jsonParseString(
    "Bearer " + crypto.signUser(user, expirationSeconds, clock.millis.toString)
  )


  val logOut: Kleisli[IO, ContextRequest[IO, AuthUser], Response[IO]] = Kleisli({ request =>
    val token = request.req.headers.get[Authorization].toList.headOption.get.credentials.toString
    println("INSERTING:" + token.replace("Bearer ", ""))
    for {
      _ <- insertTokenToDisallowList(token.replace("Bearer ", "")).transact(xa)
      resp <- Ok("Logged out")
    } yield resp
  })

  val logIn: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({ request =>
    verifyLogin(request: Request[IO]).flatMap({
      case None =>
        Forbidden(jsonParseString("Bad User Credentials"))
      case Some(user) =>
        val message = getBearer(user.name)
        Ok(message)
    })
  })

  private def verifyRegister(request: Request[IO]): IO[Option[BasicCredentials]] = for {
    res <- getCredentials(request) match
      case None =>
        IO.pure({
          None
        })
      case Some(c) =>
        verifyUserNameDoesNotExist(c)

  } yield res

  private def getCredentials(request: Request[IO]): Option[BasicCredentials] = {

    val header = request.headers.get[Authorization].toList.headOption
    for {
      h <- header
      s <- Some(BasicCredentials.apply(h.credentials.toString.split(" ")(1)))
    } yield s
  }

  private def checkUser(pass: String)(storedUser: CreatedUserData): Option[AuthUser] = {
    val isMatch = SCryptUtil.check(pass.getBytes(), storedUser.hash)
    Option.when(isMatch)(AuthUser(storedUser.uuid, storedUser.name))
  }

  private def isInDisallowList(token: String): IO[Boolean] = inDisallowListCount(token).transact(xa).map(_.contains(1))

  private def handleToken(token: String): IO[Either[String, AuthUser]] = {
    val disallowed: IO[Boolean] = isInDisallowList(token)
    val userMatch: IO[Either[String, AuthUser]] = crypto.validateSignedToken(token) match
      case None =>
        IO(Left(defaultTokenError))

      case Some((expirationTimeLong, userId)) => handleUserToken(expirationTimeLong, userId)

    for {
      isDisallowed <- disallowed
      userMatchResult <- userMatch
    } yield if (isDisallowed) Left(defaultTokenError) else userMatchResult
  }

  private def handleUserTokenRequest(request: Request[IO]): IO[Either[String, AuthUser]] = {
    val res = request.headers.get[Authorization].toList.headOption match
      case None => IO(Left(defaultTokenError))
      case Some(h) => for {
        res <- handleToken(h.credentials.toString.replace("Bearer ", ""))
      } yield res
    res
  }

  val authorizeUserFromToken: Kleisli[IO, Request[IO], Either[String, AuthUser]] = Kleisli(handleUserTokenRequest)


  private def handleUserToken(expirationTime: Long, userId: String): IO[Either[String, AuthUser]] = {
    val currentTime = clock.millis
    if (currentTime <= expirationTime) {
      retrieveUser.run(userId).map(a => toEither(a, "no user found"))
    } else {
      IO(Left(defaultTokenError))
    }
  }
}






