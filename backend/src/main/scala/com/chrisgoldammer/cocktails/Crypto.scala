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
import org.http4s.Request
import org.http4s.Response
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.http4s.server.*
import org.http4s.syntax.header.*

import com.chrisgoldammer.cocktails.cryptocore.*
import com.chrisgoldammer.cocktails.data.types.*

import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import tsec.passwordhashers.jca.*
import tsec.passwordhashers.jca.BCrypt.syncPasswordHasher

def toEither[A, B](a: Option[A], b: B): Either[B, A] =
  Either.cond(a.isDefined, a.get, b)

case class AuthFunctions(ab: AuthBackend, db: DBSetup) {
  val backend = ab.getBackingStore()
  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    db.getConnString(),
    "postgres", // user
    db.password.getOrElse("") // password
  )

  def verifyUserExists(c: BasicCredentials): IO[Option[AuthUser]] = for {
    storedUser <- backend.get(c.username).transact(xa)
  } yield storedUser.map(checkUser(c.password)).flatten.headOption

  def verifyUserNameDoesNotExist(
      c: BasicCredentials
  ): IO[Option[BasicCredentials]] = for {
    res <- backend.get(c.username).transact(xa)
  } yield Option.when(!res.isDefined)(c)

  def verifyLogin(request: Request[IO]): IO[Option[AuthUser]] = for {
    res <- getCredentials(request) match
      case None    => IO.pure(None)
      case Some(c) => verifyUserExists(c)
  } yield res

  def registerUser(user: BasicCredentials): IO[Response[IO]] = for {
    s <- backend.put(user).transact(xa)
    resp <- Ok(getBearer(user.username.toString))
  } yield resp

  val register: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({ request =>
    verifyRegister(request: Request[IO]).flatMap(_ match {
      case None =>
        Forbidden(jsonParseString("Registration failed"))
      case Some(user) => registerUser(user)
    })
  })

  def getUser(id: String): IO[Option[AuthUser]] = for {
    res <- backend.get(id).transact(xa)
  } yield res.map(toAuthUser)

  def retrieveUser: Kleisli[IO, String, Option[AuthUser]] = Kleisli(getUser)

  def jsonParseString(s: String): Json =
    jsonParse(raw""""$s"""").getOrElse(Json.Null)

  def getBearer(user: String): Json = jsonParseString(
    "Bearer " + crypto.signToken(user, clock.millis.toString)
  )

  val logIn: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({ request =>
    verifyLogin(request: Request[IO]).flatMap(_ match {
      case None =>
        Forbidden(jsonParseString("Bad User Credentials"))
      case Some(user) => {
        val message = getBearer(user.name.toString)
        Ok(message)
      }
    })
  })

  def verifyRegister(request: Request[IO]): IO[Option[BasicCredentials]] = for {
    res <- getCredentials(request) match
      case None =>
        IO.pure({
          None
        })
      case Some(c) => {
        verifyUserNameDoesNotExist(c)
      }
  } yield res

  val authorizeUserFromToken
      : Kleisli[IO, Request[IO], Either[String, AuthUser]] = Kleisli({
    request =>

      val header = request.headers.get[Authorization].toList.headOption
      println("HEADER:" + header.toString)
      val messageEither = header match
        case None => Right("No header found")
        case Some(h) =>
          crypto
            .validateSignedToken(h.credentials.toString.replace("Bearer ", ""))
            .toRight("Token invalid")
      println("Decoded:" + messageEither.toString)
      messageEither match
        case Left(error) => IO(Left(error))
        case Right(userId) =>
          retrieveUser.run(userId).map(a => toEither(a, "no user found"))
  })
}

def checkUser(pass: String)(storedUser: CreatedUserData): Option[AuthUser] = {
  val isMatch = SCryptUtil.check(pass.getBytes(), storedUser.hash)
  Option.when(isMatch)(AuthUser(storedUser.uuid, storedUser.name))
}

def getCredentials(request: Request[IO]): Option[BasicCredentials] = {

  val header = request.headers.get[Authorization].toList.headOption
  for {
    h <- header
    s <- Some(BasicCredentials.apply(h.credentials.toString.split(" ")(1)))
  } yield s
}

val key = PrivateKey(Codec.toUTF8(Random.alphanumeric.take(20).mkString("")))
val crypto = CryptoBits(PrivateKey(Codec.toUTF8(secrets.cryptocorePrivatekey)))
val clock = java.time.Clock.systemUTC
