package com.chrisgoldammer.cocktails

import cats.*
import cats.effect.*
import cats.implicits.*
import cats.data.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.server.*
import com.chrisgoldammer.cocktails.data.types.*
import com.chrisgoldammer.cocktails.cryptocore.*
import doobie.*
import doobie.implicits._

import scala.io.Codec
import scala.util.Random
import org.http4s.headers.Cookie
import org.http4s.syntax.header.*
import org.http4s.headers.Authorization
import org.http4s.client.dsl.io.*

import _root_.io.circe.{Decoder, Encoder, Json}

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
import com.chrisgoldammer.cocktails.data.types.*



def toEither[A,B](a: Option[A], b: B): Either[B, A] = Either.cond(a.isDefined, a.get, b)



case class AuthFunctions(ab: AuthBackend, db: DBSetup) {
  val backend = ab.getBackingStore()
  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    db.getConnString(),
    "postgres", // user
    "" // password
  )

  def verifyUserExists(c: BasicCredentials): IO[Option[AuthUser]] = for {
    storedUser <- backend.get(c.username).transact(xa)
  } yield storedUser.map(checkUser(c.password)).flatten.headOption

  def verifyUserNameDoesNotExist(c: BasicCredentials): IO[Option[BasicCredentials]] = for {
    res <- backend.get(c.username).transact(xa)
  } yield Option.when(!res.isDefined)(c)

  def verifyLogin(request: Request[IO]): IO[Option[AuthUser]] = for {
    res <- getCredentials(request) match
      case None => IO.pure(None)
      case Some(c) => verifyUserExists(c)
  } yield res

  def getBearer(user: String): String = "Bearer " + crypto.signToken(user, clock.millis.toString)

  def registerUser(user: BasicCredentials): IO[Response[IO]] = for {
    s <- backend.put(user).transact(xa)
    resp <- Ok(getBearer(user.username.toString))
  } yield resp

  val register: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({ request =>
    verifyRegister(request: Request[IO]).flatMap(_ match {
      case None =>
        Forbidden("B")
      case Some(user) => registerUser(user)
    })
  })

  def getUser(id: String): IO[Option[AuthUser]] = for {
    res <- backend.get(id).transact(xa)
  } yield res.map(toAuthUser)

  def retrieveUser: Kleisli[IO, String, Option[AuthUser]] = Kleisli(getUser)

  val logIn: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({ request =>
    verifyLogin(request: Request[IO]).flatMap(_ match {
      case None =>
        Forbidden("Bad User Credentials")
      case Some(user) => {
        val message = getBearer(user.name.toString)
        Ok(message)
      }
    })
  })

  def verifyRegister(request: Request[IO]): IO[Option[BasicCredentials]] = for {
    res <- getCredentials(request) match
      case None => IO.pure({
        None
      })
      case Some(c) => {
        println("Credentials: " + c)
        verifyUserNameDoesNotExist(c)
      }
  } yield res

  val authorizeUserFromToken: Kleisli[IO, Request[IO], Either[String, AuthUser]] = Kleisli({ request =>

    val header = request.headers.get[Authorization].toList.headOption
    val messageEither = header match
      case None => Right("No header found")
      case Some(h) => crypto.validateSignedToken(h.credentials.toString.replace("Bearer ", "")).toRight("Token invalid")

    messageEither match
      case Left(error) => IO(Left(error))
      case Right(userId) => retrieveUser.run(userId).map(a => toEither(a, "no user found"))
  })
}


def checkUser(pass: String)(storedUser: CreatedUserData): Option[AuthUser] = {
  val isMatch = SCryptUtil.check(pass.getBytes(), storedUser.hash)
  Option.when(isMatch)(AuthUser(storedUser.id, storedUser.name))
}


def getCredentials(request: Request[IO]): Option[BasicCredentials] = {
  
  val header = request.headers.get[Authorization].toList.headOption
  for {
    h <- header
    s <- Some(BasicCredentials.apply(h.credentials.toString.split(" ")(1)))
  } yield s
}




val key = PrivateKey(Codec.toUTF8(Random.alphanumeric.take(20).mkString("")))
val crypto = CryptoBits(key)
val clock = java.time.Clock.systemUTC










