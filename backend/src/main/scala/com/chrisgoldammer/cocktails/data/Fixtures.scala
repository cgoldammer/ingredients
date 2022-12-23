package com.chrisgoldammer.cocktails.data

import java.util.UUID.randomUUID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

import _root_.io.circe.yaml.parser
import _root_.io.circe.{parser => jsonParser}
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
  IngredientDataRaw("Rye Whiskey", List("Strong")),
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
  IngredientDataRaw("Egg White", List("Other")),
  IngredientDataRaw("Tequila", List("Strong"))
)

case class RecipeData(
    name: String,
    ingredients: List[String],
    description: String
)

implicit val decRD: Decoder[RecipeData] = deriveDecoder

val recipeData: List[RecipeData] = parser
  .parse(Source.fromResource("fixtures_recipes.yaml").getLines.mkString("\n"))
  .leftMap(err => err: _root_.io.circe.ParsingFailure)
  .flatMap(_.as[Map[String, RecipeData]])
  .valueOr(throw _)
  .values
  .toList

val user0 = BasicCredentials("testUser", "testPassword")

case class SetupDataMinimal(ingredientData: List[IngredientDataRaw],
                            recipeData: List[RecipeData])



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

case class IngredientDB(name: String)
case class CocktailDB(name: String, description: String,
                      ingredients: List[IngredientDB])

case class CocktailStored(strInstructions: String,
                          strDrink: String,
                          strIngredient1: Option[String],
                          strIngredient2: Option[String],
                          strIngredient3: Option[String],
                          strIngredient4: Option[String],
                          strIngredient5: Option[String],
                          strIngredient6: Option[String],
                          strIngredient7: Option[String],
                          strIngredient8: Option[String],
                          strIngredient9: Option[String]
                         )
implicit val fooDecoder: Decoder[CocktailStored] = deriveDecoder

case class CocktailResults(drinks: List[CocktailStored])
implicit val fooDecoder2: Decoder[CocktailResults] = deriveDecoder

def getLetterData(letter: Char): List[CocktailStored] = {
  val jsonString = Source.fromResource(s"cocktails/$letter.json").getLines.mkString("\n")
  jsonParser.decode[CocktailResults](jsonString).toOption.get.drinks
}

val excludedLetters = List('u', 'x')
val letters = ('a' to 'z').toList.filter(l => !excludedLetters.contains(l))

val resultList: List[CocktailStored] = letters.map(getLetterData).flatten

def cleanUpClass(cs: CocktailStored): RecipeData = {
  val ingList: List[String] = List(cs.strIngredient1, cs.strIngredient2,
    cs.strIngredient3, cs.strIngredient4, cs.strIngredient5,
    cs.strIngredient6, cs.strIngredient7, cs.strIngredient8, cs.strIngredient9).flatten
  return RecipeData(name=cs.strDrink,
    description=cs.strInstructions,
    ingredients=ingList)
}

val resultsDB: List[RecipeData] = resultList.map(cleanUpClass)
val ingredientsAll: List[String] = resultsDB.map(r => r.ingredients).flatten
val ingredientsMap = ingredientsAll.map(i => i -> i).toMap.transform((k, v) => v.capitalize)

def cleanUpIngredients(cd: RecipeData): RecipeData = {
  val ingredients = cd.ingredients
  val ingredientsCleaned = ingredients.map(ingredientsMap)
  RecipeData(name = cd.name,
    description = cd.description,
    ingredients = ingredientsCleaned)
}

val tagData: Map[String, List[String]] = parser
  .parse(Source.fromResource("ingredient_tags.yaml").getLines.mkString("\n"))
  .leftMap(err => err: _root_.io.circe.ParsingFailure)
  .flatMap(_.as[Map[String, List[String]]])
  .valueOr(throw _)

def toIngredientData(i: String): IngredientDataRaw = {
  IngredientDataRaw(i, tagData.getOrElse(i, List()))
}
val resultsCleanedDB: List[RecipeData] = resultsDB.map(cleanUpIngredients)
val ingredientDataDB: List[IngredientDataRaw] = resultsCleanedDB.map(r => r.ingredients.map(toIngredientData)).flatten.distinct
val setupDataDB = SetupData(ingredientDataDB, resultsCleanedDB, Map(), List())


