package com.chrisgoldammer.cocktails.data.types

import doobie.*
import doobie.implicits.*
import doobie.util.ExecutionContexts
import cats.*
import cats.data.*
import cats.effect.*
import cats.implicits.*
import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext.Implicits.global
import doobie.postgres.*
import doobie.postgres.implicits.*
import com.chrisgoldammer.cocktails.data.types.*
import com.chrisgoldammer.cocktails.data.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.EntityDecoder


import _root_.io.circe.*
import _root_.io.circe.generic.semiauto.*
import _root_.io.circe.syntax.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.client.dsl.io.*
import org.http4s.*
import org.http4s.circe.jsonOf

case class StoredElement[T](id: Int, element: T)

case class Ingredient(name: String, uuid: String)
implicit val sReadIngredient: Read[StoredElement[Ingredient]] =
  Read[(Int, String, String)].map { case (id, name, uuid) => new StoredElement(id, Ingredient(name, uuid))}

case class IngredientSet(name: String, uuid: String)
implicit val sReadIngredientSet: Read[StoredElement[IngredientSet]] =
  Read[(Int, String, String)].map { case (id, name, uuid) => new StoredElement(id, IngredientSet(name, uuid))}

case class IngredientSetIngredient(setId: Int, ingredientId: Int)
implicit val sReadIngredientSetIngredient: Read[StoredElement[IngredientSetIngredient]] =
  Read[(Int, Int, Int)].map { case (id, setId, ingredientId) => new StoredElement(id, IngredientSetIngredient(setId, ingredientId))}


case class Recipe(name: String, uuid: String)
implicit val sReadRecipe: Read[StoredElement[Recipe]] =
  Read[(Int, String, String)].map { case (id, name, uuid) => new StoredElement(id, Recipe(name, uuid))}

case class RecipeIngredient(recipeId: Int, ingredientId: Int)
implicit val sReadRecipeIngredient: Read[StoredElement[RecipeIngredient]] =
  Read[(Int, Int, Int)].map { case (id, recipeId, ingredientId) => new StoredElement(id, RecipeIngredient(recipeId, ingredientId))}

case class Tag(name: String)
implicit val sReadTag: Read[StoredElement[Tag]] =
  Read[(Int, String)].map { case (id, name) => new StoredElement(id, Tag(name))}

case class IngredientTag(ingredientId: Int, tagId: Int)
implicit val sReadIngredientTag: Read[StoredElement[IngredientTag]] =
  Read[(Int, Int, Int)].map { case (id, ingredientId, tagId) => new StoredElement(id, IngredientTag(ingredientId, tagId))}

case class FullRecipe(name: String, uuid: String, ingredients: List[Ingredient])
case class FullRecipeData(name: String, uuid: String, ingredientName: String, ingredientUuid: String)
case class FullIngredient(name: String, uuid: String, tags: List[Tag])

case class FullIngredientSet(name: String, uuid: String, ingredients: List[String])
implicit val sReadFullIngredientSet: Read[StoredElement[FullIngredientSet]] =
  Read[(Int, String, String, List[String])].map { case (id, name, uuid, ingredients) => new StoredElement(id, FullIngredientSet(name, uuid, ingredients))}


case class MFullIngredientData(id: Int, name: String, uuid: String, tags: List[String])
case class MFullRecipe(id: Int, uuid: String, name: String, ingredients: List[StoredElement[Ingredient]])

case class Results[T](data: List[T], name: String = "Default")

case class IngredientDataRaw(name: String, IngredientTagNames: List[String])

implicit val decIS: Decoder[IngredientSet] = deriveDecoder
implicit val encIS: Encoder[IngredientSet] = deriveEncoder

implicit val decT: Decoder[Tag] = deriveDecoder
implicit val encT: Encoder[Tag] = deriveEncoder

implicit val decTR: Decoder[Results[Tag]] = deriveDecoder
implicit val encTR: Encoder[Results[Tag]] = deriveEncoder
implicit val decT2: EntityDecoder[IO, Results[Tag]] = jsonOf[IO, Results[Tag]]


implicit val decI: Decoder[Ingredient] = deriveDecoder
implicit val encI: Encoder[Ingredient] = deriveEncoder

implicit val decIR: Decoder[Results[Ingredient]] = deriveDecoder
implicit val encIR: Encoder[Results[Ingredient]] = deriveEncoder
implicit val decIR2: EntityDecoder[IO, Results[Ingredient]] = jsonOf[IO, Results[Ingredient]]


implicit val decFI: Decoder[FullIngredient] = deriveDecoder
implicit val encFI: Encoder[FullIngredient] = deriveEncoder

implicit val decRR9: Decoder[Results[FullIngredient]] = deriveDecoder
implicit val encRR9: Encoder[Results[FullIngredient]] = deriveEncoder
implicit val decRR99: EntityDecoder[IO, Results[FullIngredient]] = jsonOf[IO, Results[FullIngredient]]

implicit val decMR: Decoder[Recipe] = deriveDecoder
implicit val encMR: Encoder[Recipe] = deriveEncoder

implicit val decRR: Decoder[Results[Recipe]] = deriveDecoder
implicit val encRR: Encoder[Results[Recipe]] = deriveEncoder
implicit val decRR2: EntityDecoder[IO, Results[Recipe]] = jsonOf[IO, Results[Recipe]]

implicit val decI2: Decoder[FullRecipe] = deriveDecoder
implicit val encI2: Encoder[FullRecipe] = deriveEncoder

implicit val decI3: Decoder[Results[FullRecipe]] = deriveDecoder
implicit val encI3: Encoder[Results[FullRecipe]] = deriveEncoder
implicit val decIR23: EntityDecoder[IO, Results[FullRecipe]] = jsonOf[IO, Results[FullRecipe]]

implicit val decI24: Decoder[FullIngredientSet] = deriveDecoder
implicit val encI24: Encoder[FullIngredientSet] = deriveEncoder

implicit val decRR4: Decoder[Results[FullIngredientSet]] = deriveDecoder
implicit val encRR4: Encoder[Results[FullIngredientSet]] = deriveEncoder
implicit val decRR24: EntityDecoder[IO, Results[FullIngredientSet]] = jsonOf[IO, Results[FullIngredientSet]]


implicit val decISL: Decoder[Results[String]] = deriveDecoder
implicit val encISL: Encoder[Results[String]] = deriveEncoder
implicit val decISL2: EntityDecoder[IO, Results[String]] = jsonOf[IO, Results[String]]
