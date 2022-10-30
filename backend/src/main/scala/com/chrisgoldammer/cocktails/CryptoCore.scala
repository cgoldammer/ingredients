package com.chrisgoldammer.cocktails.cryptocore

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import com.chrisgoldammer.cocktails.data.types.{AuthUser, DBSetup}
import org.apache.commons.codec.binary.Hex
import org.http4s.{BasicCredentials, Request}
import tsec.passwordhashers.jca.*
import java.util.UUID.randomUUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.collection.mutable
import scala.util.Random
import doobie.*
import doobie.implicits.*
import doobie.hi.connection
import doobie.postgres._
import doobie.postgres.implicits._

import java.util.UUID.randomUUID

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




val user = AuthUser(id = "fff", name = "TestUser")

def getUuid() = randomUUID().toString

case class CreatedUserData(id: String, name: String, hash: String) {
}

def toAuthUser(that: CreatedUserData): AuthUser = AuthUser(that.id, that.name)

val createdUser = CreatedUserData(user.id, user.name, "$s0$e0801$jSIPEy4Ow8LouWdRNmEydA==$4M2oXnkqIPtT8VcU1LU+kDdgwbV893W9UvPi4oWH/A4=")

val password = "helloworld"
val userCreationData = BasicCredentials(user.name, password)


trait BackingStore {
  def asString(): String
  def get(id: String): ConnectionIO[Option[CreatedUserData]]
  def put(elem: BasicCredentials): ConnectionIO[Option[CreatedUserData]]
  def update(v: CreatedUserData): ConnectionIO[CreatedUserData]
  def delete(id: String): ConnectionIO[Boolean]
}

enum AuthBackend:
  case Doobie, Local

  def getBackingStore(): BackingStore = {
    this match
      case AuthBackend.Doobie => dummyBackingStore()
  }

// TODO: Create a way that the backingstore is a parameter on app startup
object AuthHelpers {
  def hashPassword(pass: String): String = SCrypt.hashpwUnsafe(pass.getBytes())
}

//def ioToConnection[A](io: IO[A]): ConnectionIO[A] = WeakAsync.liftK[IO, ConnectionIO]


def dummyBackingStore(): BackingStore = {
  val bStore = new BackingStore {
    val storageMap = mutable.HashMap.empty[String, CreatedUserData]

    def getRandom(): String = Random.alphanumeric.take(20).mkString("")

    def asString(): String = storageMap.toString()
    def getCreated(c: BasicCredentials): CreatedUserData = CreatedUserData(getRandom(), c.username, AuthHelpers.hashPassword(c.password))

    def put(elem: BasicCredentials): ConnectionIO[Option[CreatedUserData]] = {
      val cu = getCreated(elem)
      val name = cu.name
      val uuid = getUuid()
      val hash = cu.hash
      for {
        p <- sql"insert into users (name, uuid, hash, is_admin) values ($name, $uuid, $hash, false)".updateWithLogHandler(LogHandler.jdkLogHandler).withUniqueGeneratedKeys[String]("name")
      } yield Some(cu)
    }

    def get(id: String): ConnectionIO[Option[CreatedUserData]] =
    {
      for {
        p <- sql"select uuid, name, hash from users where name=$id".queryWithLogHandler[CreatedUserData](LogHandler.jdkLogHandler).to[List].map(_.headOption)
      } yield p
    }
//
    def update(v: CreatedUserData): ConnectionIO[CreatedUserData] = ???
//          IO({
//          storageMap.update(v.name, v)
//          v
//        })
//
    def delete(id: String): ConnectionIO[Boolean] = ???
    //      storageMap.remove(id) match {
    //        case Some(_) => IO(true)
    //        case None => IO(false)
    //      }
  }

  return bStore
}