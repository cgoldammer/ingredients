package com.chrisgoldammer.cocktails.data.types

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time
import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext.Implicits.global
import _root_.io.circe.Decoder
import _root_.io.circe.Encoder
import _root_.io.circe.Json
import _root_.io.circe.generic.auto.*
import _root_.io.circe.generic.semiauto.deriveDecoder
import _root_.io.circe.generic.semiauto.deriveEncoder
import _root_.io.circe.syntax.EncoderOps
import cats.effect.IO
import doobie.LogHandler
import doobie.Read
import doobie.postgres.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.log.LogEvent
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.http4s.client.dsl.io.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import com.chrisgoldammer.cocktails.data.*
import com.chrisgoldammer.cocktails.data.types.*
import _root_.io.circe.yaml.parser
import cats.implicits.catsSyntaxEither
import doobie.Transactor
import doobie.implicits.toConnectionIOOps

import scala.io.Source

case class StoredElement[T](id: Int, element: T)

case class Ingredient(name: String, uuid: String)
implicit val sReadIngredient: Read[StoredElement[Ingredient]] =
  Read[(Int, String, String)].map { case (id, name, uuid) =>
     StoredElement(id, Ingredient(name, uuid))
  }

case class IngredientSet(name: String, uuid: String)
implicit val sReadIngredientSet: Read[StoredElement[IngredientSet]] =
  Read[(Int, String, String)].map { case (id, name, uuid) =>
     StoredElement(id, IngredientSet(name, uuid))
  }

case class IngredientSetIngredient(setId: Int, ingredientId: Int)
implicit
val sReadIngredientSetIngredient: Read[StoredElement[IngredientSetIngredient]] =
  Read[(Int, Int, Int)].map { case (id, setId, ingredientId) =>
     StoredElement(id, IngredientSetIngredient(setId, ingredientId))
  }

case class Recipe(name: String, uuid: String)
implicit val sReadRecipe: Read[StoredElement[Recipe]] =
  Read[(Int, String, String)].map { case (id, name, uuid) =>
     StoredElement(id, Recipe(name, uuid))
  }

case class RecipeIngredient(recipeId: Int, ingredientId: Int)
implicit val sReadRecipeIngredient: Read[StoredElement[RecipeIngredient]] =
  Read[(Int, Int, Int)].map { case (id, recipeId, ingredientId) =>
     StoredElement(id, RecipeIngredient(recipeId, ingredientId))
  }

case class Tag(name: String)
implicit val sReadTag: Read[StoredElement[Tag]] =
  Read[(Int, String)].map { case (id, name) =>
     StoredElement(id, Tag(name))
  }

case class IngredientTag(ingredientId: Int, tagId: Int)
implicit val sReadIngredientTag: Read[StoredElement[IngredientTag]] =
  Read[(Int, Int, Int)].map { case (id, ingredientId, tagId) =>
     StoredElement(id, IngredientTag(ingredientId, tagId))
  }

case class FullRecipe(
    name: String,
    uuid: String,
    description: String,
    ingredients: List[Ingredient]
)
case class FullRecipeData(
    name: String,
    uuid: String,
    description: String,
    ingredientName: String,
    ingredientUuid: String
)
case class FullIngredient(name: String, uuid: String, tags: List[Tag], numberRecipes: Int)

case class FullIngredientSet(
    name: String,
    uuid: String,
    ingredients: List[String]
)
implicit val sReadFullIngredientSet: Read[StoredElement[FullIngredientSet]] =
  Read[(Int, String, String, List[String])].map {
    case (id, name, uuid, ingredients) =>
       StoredElement(id, FullIngredientSet(name, uuid, ingredients))
  }

case class MFullIngredientData(
    id: Int,
    name: String,
    uuid: String,
    tags: List[String],
    numberRecipes: Int
)
case class MFullRecipe(
    id: Int,
    uuid: String,
    name: String,
    ingredients: List[StoredElement[Ingredient]]
)

case class InsertIngredientSetData(
    setName: String,
    ingredientUuids: List[String]
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

implicit val decISD: Decoder[InsertIngredientSetData] = deriveDecoder
implicit val encISD: Encoder[InsertIngredientSetData] = deriveEncoder
implicit val decISD2: EntityDecoder[IO, InsertIngredientSetData] =
  jsonOf[IO, InsertIngredientSetData]

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
    dbName: String = "ingredients_dev",
    password: Option[String],
    user: String = "postgres"
) {
  def getConnString: String = s"jdbc:postgresql://$serverName:$port/$dbName"
}

enum Settings:
  case TestLocal, DevLocal, DevDocker, Prod

  private def toStringLower: String = camel2underscores(this.toString())
  private def toDBName: String = this.toStringLower.split('_')(0)

  def getSetup: DBSetup = {
    val dbName = "ingredients_" + this.toDBName

    this match
      case TestLocal => DBSetup(dbName = dbName, password=None  )
      case DevLocal  => DBSetup(dbName = dbName, password=None )
      case DevDocker => DBSetup(dbName = dbName, serverName = "postgres2", password=None )
      case Prod => DBSetup(dbName=dbName, serverName = secrets.postgresAwsUrl, password=Some(secrets.postgresAwsPassword))
  }

object Settings:
  def fromString(s: String): Option[Settings] = {
    s match
      case "testLocal" => Some(Settings.TestLocal)
      case "devLocal"  => Some(Settings.DevLocal)
      case "devDocker" => Some(Settings.DevDocker)
      case "prod"      => Some(Settings.Prod)
      case _           => None
  }

def getSettings: Option[Settings] =
  sys.env.get("SETTINGS").flatMap(Settings.fromString)

def getTransactor(dbSetup: DBSetup) = {
  Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    dbSetup.getConnString,
    "postgres", // user
    dbSetup.password.getOrElse("") // password
  )
}

def fileLogHandler(le: LogEvent): Unit = {
  val settings = getSettings
  val logString = time.LocalDateTime.now().toString + ": " + le.sql + "\n"
  val logFile =
    "logs/log_" + settings.toString.replace('(', '_').replace(')', '_')
  val path = Paths.get(logFile)
  if (!Files.exists(path)){
    Files.createFile(path)
  }
  Files.writeString(path, logString, StandardOpenOption.APPEND)
}

implicit val logHandler: LogHandler = LogHandler(fileLogHandler)

case class Secrets(
                    postgresAwsPassword: String,
                    postgresAwsUrl: String,
                    cryptocorePrivatekey: String
                  )

implicit val decS: Decoder[Secrets] = deriveDecoder

val secrets: Secrets = parser
  .parse(Source.fromResource("secrets.yaml").getLines.mkString("\n"))
  .leftMap(err => err: io.circe.ParsingFailure)
  .flatMap(_.as[Secrets])
  .valueOr(throw _)