package com.chrisgoldammer.cocktails.data

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
import org.http4s.{BasicCredentials, Request}
val homeIngredients = List("Gin", "Vodka", "Bourbon", "Orange Liquour",
  "Scotch", "Aperol", "Dry Vermouth", "Campari", "Sugar", "Bitters", "Egg White")
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
  IngredientDataRaw("Egg White", List("Other")),
)
val recipeNames = Map(
  "Boulevardier" -> List("Bourbon", "Dry Vermouth", "Campari"),
  "Old Fashioned" -> List("Bourbon", "Sugar", "Bitters"),
  "White Lady" -> List("Gin", "Orange Liquour", "Lemon Juice", "Egg White"),
  "Negroni" -> List("Gin", "Campari", "Sweet Vermouth")
)

val user0 = BasicCredentials("testUser", "testPassword")

case class SetupData(ingredientData: List[IngredientDataRaw]
                     , recipeNames: Map[String, List[String]]
                     , ingredientSets: Map[String, List[String]]
                    , users: List[BasicCredentials])
val setupDataSimple = SetupData(ingredientData = ingredientData, recipeNames = recipeNames, ingredientSets = ingredientSets, users=List(user0))