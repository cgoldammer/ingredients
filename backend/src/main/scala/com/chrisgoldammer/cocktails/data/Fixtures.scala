package com.chrisgoldammer.cocktails.data

import java.util.UUID.randomUUID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

import _root_.io.circe.yaml.parser
import cats.effect.IO
import cats.implicits.catsSyntaxEither
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.ExecutionContexts
import org.http4s.BasicCredentials
import org.http4s.EntityDecoder
import org.http4s.Request

import com.chrisgoldammer.cocktails.data.*
import com.chrisgoldammer.cocktails.data.types.*

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.parser

val homeIngredients = List(
  "Gin",
  "Vodka",
  "Bourbon",
  "Orange Liquour",
  "Scotch",
  "Aperol",
  "Dry Vermouth",
  "Campari",
  "Sugar",
  "Bitters",
  "Egg White"
)
val ingredientSets = Map(
  "home" -> homeIngredients
)

val ingredientData: List[IngredientDataRaw] = List(
  IngredientDataRaw("Gin", List("Strong")),
  IngredientDataRaw("Vodka", List("Strong")),
  IngredientDataRaw("Bourbon", List("Strong")),
  IngredientDataRaw("Orange Liquour", List("Liquour")),
  IngredientDataRaw("Rye", List("Strong")),
  IngredientDataRaw("Scotch", List("Strong")),
  IngredientDataRaw("Dry Vermouth", List("Fortified Wine")),
  IngredientDataRaw("Sweet Vermouth", List("Fortified Wine")),
  IngredientDataRaw("Campari", List("Other alcohol")),
  IngredientDataRaw("Aperol", List("Other alcohol")),
  IngredientDataRaw("Sugar", List("Sugar")),
  IngredientDataRaw("Bitters", List("Bitter")),
  IngredientDataRaw("Lemon Juice", List("Juice")),
  IngredientDataRaw("Egg White", List("Other"))
)

val yaml =
  Source.fromResource("./fixtures_recipes.yaml").getLines.mkString("\n")

val json = parser.parse(yaml)
case class RecipeData(
    name: String,
    ingredients: List[String],
    description: String
)

implicit val decRD: Decoder[RecipeData] = deriveDecoder

val recipeData: List[RecipeData] = json
  .leftMap(err => err: io.circe.ParsingFailure)
  .flatMap(_.as[Map[String, RecipeData]])
  .valueOr(throw _)
  .values
  .toList

val recipeNames: Map[String, List[String]] = Map(
  "Boulevardier" -> List("Bourbon", "Dry Vermouth", "Campari"),
  "Old Fashioned" -> List("Bourbon", "Sugar", "Bitters"),
  "White Lady" -> List("Gin", "Orange Liquour", "Lemon Juice", "Egg White"),
  "Negroni" -> List("Gin", "Campari", "Sweet Vermouth")
)

val user0 = BasicCredentials("testUser", "testPassword")

case class SetupData(
    ingredientData: List[IngredientDataRaw],
    recipeData: List[RecipeData],
    ingredientSets: Map[String, List[String]],
    users: List[BasicCredentials]
)
val setupDataSimple = SetupData(
  ingredientData = ingredientData,
  recipeData = recipeData,
  ingredientSets = ingredientSets,
  users = List(user0)
)
