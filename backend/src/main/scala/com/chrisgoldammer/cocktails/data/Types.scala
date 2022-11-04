package com.chrisgoldammer.cocktails.data.types

import doobie.{LogHandler, Read}
import doobie.util.{ExecutionContexts}
import cats.effect.{IO}
import java.nio.file.{Files, Paths, StandardOpenOption}

import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext.Implicits.global
import doobie.postgres.implicits.*

import _root_.io.circe.{Decoder, Encoder, Json}
import _root_.io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import _root_.io.circe.generic.auto.*
import io.circe.syntax.EncoderOps
import doobie.util.log.LogEvent
import org.http4s.EntityDecoder
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.client.dsl.io.*
import org.http4s.circe.jsonOf

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time

import com.chrisgoldammer.cocktails.data.types.*
import com.chrisgoldammer.cocktails.data.*

case class StoredElement[T](id: Int, element: T)

case class Ingredient(name: String, uuid: String)
implicit val sReadIngredient: Read[StoredElement[Ingredient]] =
  Read[(Int, String, String)].map { case (id, name, uuid) =>
    new StoredElement(id, Ingredient(name, uuid))
  }

case class IngredientSet(name: String, uuid: String)
implicit val sReadIngredientSet: Read[StoredElement[IngredientSet]] =
  Read[(Int, String, String)].map { case (id, name, uuid) =>
    new StoredElement(id, IngredientSet(name, uuid))
  }

case class IngredientSetIngredient(setId: Int, ingredientId: Int)
implicit
val sReadIngredientSetIngredient: Read[StoredElement[IngredientSetIngredient]] =
  Read[(Int, Int, Int)].map { case (id, setId, ingredientId) =>
    new StoredElement(id, IngredientSetIngredient(setId, ingredientId))
  }

case class Recipe(name: String, uuid: String)
implicit val sReadRecipe: Read[StoredElement[Recipe]] =
  Read[(Int, String, String)].map { case (id, name, uuid) =>
    new StoredElement(id, Recipe(name, uuid))
  }

case class RecipeIngredient(recipeId: Int, ingredientId: Int)
implicit val sReadRecipeIngredient: Read[StoredElement[RecipeIngredient]] =
  Read[(Int, Int, Int)].map { case (id, recipeId, ingredientId) =>
    new StoredElement(id, RecipeIngredient(recipeId, ingredientId))
  }

case class Tag(name: String)
implicit val sReadTag: Read[StoredElement[Tag]] =
  Read[(Int, String)].map { case (id, name) =>
    new StoredElement(id, Tag(name))
  }

case class IngredientTag(ingredientId: Int, tagId: Int)
implicit val sReadIngredientTag: Read[StoredElement[IngredientTag]] =
  Read[(Int, Int, Int)].map { case (id, ingredientId, tagId) =>
    new StoredElement(id, IngredientTag(ingredientId, tagId))
  }

case class FullRecipe(name: String, uuid: String, ingredients: List[Ingredient])
case class FullRecipeData(
    name: String,
    uuid: String,
    ingredientName: String,
    ingredientUuid: String
)
case class FullIngredient(name: String, uuid: String, tags: List[Tag])

case class FullIngredientSet(
    name: String,
    uuid: String,
    ingredients: List[String]
)
implicit val sReadFullIngredientSet: Read[StoredElement[FullIngredientSet]] =
  Read[(Int, String, String, List[String])].map {
    case (id, name, uuid, ingredients) =>
      new StoredElement(id, FullIngredientSet(name, uuid, ingredients))
  }

case class MFullIngredientData(
    id: Int,
    name: String,
    uuid: String,
    tags: List[String]
)
case class MFullRecipe(
    id: Int,
    uuid: String,
    name: String,
    ingredients: List[StoredElement[Ingredient]]
)

case class Results[T](data: List[T], name: String = "Default")

case class IngredientDataRaw(name: String, IngredientTagNames: List[String])

implicit val encTR: Encoder[Results[Tag]] = deriveEncoder
implicit val decT2: EntityDecoder[IO, Results[Tag]] = jsonOf[IO, Results[Tag]]
implicit val decIR2: EntityDecoder[IO, Results[Ingredient]] =
  jsonOf[IO, Results[Ingredient]]
implicit val decRR433: Decoder[FullIngredient] = deriveDecoder

implicit val decRR99: EntityDecoder[IO, Results[FullIngredient]] =
  jsonOf[IO, Results[FullIngredient]]
implicit val decRR: Decoder[Results[Recipe]] = deriveDecoder
implicit val encRR: Encoder[Results[Recipe]] = deriveEncoder
implicit val decRR2: EntityDecoder[IO, Results[Recipe]] =
  jsonOf[IO, Results[Recipe]]
implicit val decI3: Decoder[Results[FullRecipe]] = deriveDecoder
implicit val encI3: Encoder[Results[FullRecipe]] = deriveEncoder
implicit val decIR23: EntityDecoder[IO, Results[FullRecipe]] =
  jsonOf[IO, Results[FullRecipe]]

//implicit val encX: Encoder[FullIngredientSet] = deriveEncoder
implicit val decRR4: Decoder[Results[FullIngredientSet]] = deriveDecoder
implicit val encRR4: Encoder[Results[FullIngredientSet]] = deriveEncoder
implicit val decRR24: EntityDecoder[IO, Results[FullIngredientSet]] =
  jsonOf[IO, Results[FullIngredientSet]]

implicit val decRR42: Decoder[Results[FullIngredient]] = deriveDecoder
implicit val encRR42: Encoder[Results[FullIngredient]] = deriveEncoder

implicit val decISL: Decoder[Results[String]] = deriveDecoder
implicit val encISL: Encoder[Results[String]] = deriveEncoder
implicit val decISL2: EntityDecoder[IO, Results[String]] =
  jsonOf[IO, Results[String]]

case class AuthUser(id: String, name: String)
implicit val encAU: Encoder[AuthUser] = deriveEncoder
implicit val decAU4: Decoder[AuthUser] = deriveDecoder
implicit val decAU2: EntityDecoder[IO, AuthUser] = jsonOf[IO, AuthUser]

case class LoginResponse(body: String)
implicit val decLR: Decoder[LoginResponse] = deriveDecoder
implicit val encLR: Encoder[LoginResponse] = deriveEncoder
implicit val decLR2: EntityDecoder[IO, LoginResponse] =
  jsonOf[IO, LoginResponse]

def loginResponseJson(body: String): Json = LoginResponse(body).asJson

implicit val decJS: EntityDecoder[IO, Json] = jsonOf[IO, Json]

case class DBSetup(
    port: Int = 5432,
    serverName: String = "localhost",
    dbName: String = "ingradients_dev"
) {
  def getConnString(): String = s"jdbc:postgresql://$serverName:$port/$dbName"
}

enum Settings:
  case TestLocal, DevLocal, DevDocker

  def toStringLower(): String = camel2underscores(this.toString())
  def toDBName(): String = this.toStringLower().split('_')(0)

  def getSetup(): DBSetup = {
    val dbName = "ingredients_" + this.toDBName()

    this match
      case TestLocal => DBSetup(dbName = dbName)
      case DevLocal  => DBSetup(dbName = dbName)
      case DevDocker => DBSetup(dbName = dbName, serverName = "postgres2")
  }

object Settings:
  def fromString(s: String): Option[Settings] = {
    s match
      case "testLocal" => Some(Settings.TestLocal)
      case "devLocal"  => Some(Settings.DevLocal)
      case "devDocker" => Some(Settings.DevDocker)
      case _           => None
  }

def getSettings(): Option[Settings] =
  sys.env.get("SETTINGS").map(Settings.fromString).flatten

def fileLogHandler(le: LogEvent): Unit = {
  val settings = getSettings()
  val logString = time.LocalDateTime.now().toString + ": " + le.sql + "\n"
  val logFile = "logs/log_" + settings.toString
  Files.writeString(Paths.get(logFile), logString, StandardOpenOption.APPEND)
}

implicit val logHandler: LogHandler = LogHandler(fileLogHandler)
