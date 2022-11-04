package com.chrisgoldammer.cocktails.data

import java.util.UUID.randomUUID

import scala.concurrent.ExecutionContext.Implicits.global

import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.ExecutionContexts
import org.http4s.BasicCredentials
import org.http4s.Request

import com.chrisgoldammer.cocktails.data.*
import com.chrisgoldammer.cocktails.data.types.*

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
val recipeNames = Map(
  "Boulevardier" -> List("Bourbon", "Dry Vermouth", "Campari"),
  "Old Fashioned" -> List("Bourbon", "Sugar", "Bitters"),
  "White Lady" -> List("Gin", "Orange Liquour", "Lemon Juice", "Egg White"),
  "Negroni" -> List("Gin", "Campari", "Sweet Vermouth")
)

val user0 = BasicCredentials("testUser", "testPassword")

case class SetupData(
    ingredientData: List[IngredientDataRaw],
    recipeNames: Map[String, List[String]],
    ingredientSets: Map[String, List[String]],
    users: List[BasicCredentials]
)
val setupDataSimple = SetupData(
  ingredientData = ingredientData,
  recipeNames = recipeNames,
  ingredientSets = ingredientSets,
  users = List(user0)
)
