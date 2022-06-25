package com.chrisgoldammer.cocktails.data

import doobie.*
import doobie.implicits.*
import doobie.util.ExecutionContexts
import cats.*
import cats.data.*
import cats.effect.*
import cats.implicits.*
import cats.effect.unsafe.implicits.global
import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext.Implicits.global

def getUuid() = randomUUID().toString

val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver",     // driver classname
  "jdbc:postgresql:world",     // connect URL (driver-specific)
  "postgres",                  // user
  ""                           // password
)

val createIngredients =
  sql"""
CREATE TABLE ingredients(
id SERIAL,
name VARCHAR NOT NULL UNIQUE,
uuid VARCHAR NOT NULL UNIQUE,
PRIMARY KEY(id)
)
""".stripMargin

val createRecipe =
  sql"""
CREATE TABLE recipes (
id SERIAL,
name VARCHAR NOT NULL UNIQUE,
uuid VARCHAR NOT NULL UNIQUE,
PRIMARY KEY(id)
)
""".stripMargin

val createRecipeIngredients =
  sql"""
CREATE TABLE recipe_ingredients (
id SERIAL,
recipe_id INT,
ingredient_id INT,
PRIMARY KEY(id),
CONSTRAINT fk_ingredient FOREIGN KEY(ingredient_id) REFERENCES ingredients(id),
CONSTRAINT fk_recipe FOREIGN KEY(recipe_id) REFERENCES recipes(id)
)
""".stripMargin


case class MElement[T](id: Int, element: T)

case class Ingredient(name: String, uuid: String)
case class Recipe(name: String, uuid: String)
case class RecipeIngredient()
case class FullRecipe(name: String, ingredients: Array[Ingredient])
case class RecipeIngredientData(name: String, ingredientName: String, ingredientUuid: String)

case class MIngredient(id: Int, element: Ingredient)
case class MIngredientData(id: Int, name: String, uuid: String)

case class MRecipeIngredient(id: Int, element: RecipeIngredient)
case class MRecipeIngredientData(id: Int, recipeId: Int, ingredientId: Int)

case class MRecipe(id: Int, element: Recipe)
case class MRecipeData(id: Int, name: String, uuid: String)

case class MFullRecipe(id: Int, name: String, ingredients: Array[MIngredient])

case class IngredientResult(ingredients: Array[Ingredient])
sealed case class IngredientSearchList(ingredients: Array[String])


def getMIngredientFromData(md: MIngredientData): MIngredient = {
  val ingredient = Ingredient(name=md.name, uuid=md.uuid)
  MIngredient(id=md.id, element=ingredient)
}

def getMRecipeFromData(md: MRecipeData): MRecipe = {
  val recipe = Recipe(name=md.name, uuid=md.uuid)
  MRecipe(id=md.id, element=recipe)
}

def insertIngredient(name: String): ConnectionIO[MIngredient] = {
  val uuid = getUuid()
  sql"INSERT INTO ingredients (name, uuid) values ($name, $uuid)".update.withUniqueGeneratedKeys("id", "name", "uuid")
}

def insertRecipeIngredient(recipeId: Int, ingredientId: Int): ConnectionIO[MRecipeIngredientData] = {
  sql"INSERT INTO recipe_ingredients (recipe_id, ingredient_id) values ($recipeId, $ingredientId)"
    .update.withUniqueGeneratedKeys("id", "recipe_id", "ingredient_id")
}

def insertRecipe(name: String): ConnectionIO[MRecipeData] = {
  val uuid = getUuid()
  sql"INSERT INTO recipes (name, uuid) values ($name, $uuid)".update.withUniqueGeneratedKeys("id", "name", "uuid")
}

def recipeIngredientToIngredient(ri: RecipeIngredientData): Ingredient = {
  ri match {
    case RecipeIngredientData(_, ingredientName, ingredientUuid) => Ingredient(ingredientName, ingredientUuid)
  }
}

def createFullRecipes(rid: Array[RecipeIngredientData]): Array[FullRecipe] = {
  val grouped = rid.groupBy(_.name)
  grouped.toArray.map{case (name, ri) => FullRecipe(name, ri.map(recipeIngredientToIngredient))}
}

def dropTables(): Unit = {
  sql"DROP TABLE IF EXISTS recipe_ingredients".update.run.transact(xa).unsafeRunSync()
  sql"DROP TABLE IF EXISTS recipes".update.run.transact(xa).unsafeRunSync()
  sql"DROP TABLE IF EXISTS ingredients".update.run.transact(xa).unsafeRunSync()
}

def createTables(): Unit = {
  createRecipe.update.run.transact(xa).unsafeRunSync()
  createIngredients.update.run.transact(xa).unsafeRunSync()
  createRecipeIngredients.update.run.transact(xa).unsafeRunSync()
}

val recipeIngredientDataQuery =
  sql"""
       |SELECT r.name, i.name AS ingredient_name
FROM recipe_ingredients ri
JOIN recipes r ON ri.recipe_id=r.id
JOIN ingredients i ON ri.ingredient_id=i.id
""".stripMargin


class DataHelper(val mIngredients: Array[MIngredient], val mRecipes: Array[MRecipe]){

  val recipesByName = mRecipes.groupBy(_.element.name).transform((k, v) => v.head)
  val ingredientsByName = mIngredients.groupBy(_.element.name).transform((k, v) => v.head)

  def getRecipe(name: String): MRecipe ={
    recipesByName(name)
  }
  def getIngredient(name: String): MIngredient ={
    ingredientsByName(name)
  }
}

case class SetupData(ingredientNames: Array[String], recipeNames: Map[String, Array[String]])

def insertFromSetupData(sd: SetupData): Unit = {
  val mIngredients =
    for (ingredient <- sd.ingredientNames)
      yield insertIngredient(ingredient).transact(xa).unsafeRunSync()
  val mRecipeDatas =
    for (recipe <- sd.recipeNames.keys)
      yield insertRecipe(recipe).transact(xa).unsafeRunSync()

  val dh = DataHelper(mIngredients=mIngredients.toArray, mRecipes=mRecipeDatas.toArray.map(getMRecipeFromData))

  val mRecipeIngredients = for {
    (recipeName, recipeIngredientNames) <- sd.recipeNames
    recipeIngredientName <- recipeIngredientNames.toList
  } yield {
    val recipeId = dh.getRecipe(recipeName).id
    val ingredientId = dh.getIngredient(recipeIngredientName).id
    insertRecipeIngredient(recipeId=recipeId, ingredientId=ingredientId).transact(xa).unsafeRunSync()
  }
}

def searchQuery(ingredientUuids: NonEmptyList[String]) = {
  val inFragment = Fragments.in(fr"i.uuid", ingredientUuids)
  val numIngCount = ingredientUuids.size
  fr"""
  WITH candidates AS (
    SELECT recipe_id
    FROM recipe_ingredients ri
    JOIN ingredients i
    ON ri.ingredient_id=i.id
    WHERE
    """ ++ inFragment ++ fr"""
    ),
    agg AS (
      SELECT recipe_id, SUM(1) AS num_ing
      FROM candidates
      GROUP BY recipe_id
    )
    SELECT id, name, uuid FROM recipes
    WHERE id IN (SELECT recipe_id FROM agg WHERE num_ing = ${numIngCount})
    """
}

def getRecipesForIngredients(ingredientUids: Array[String]): Array[MRecipeData] = {
  if (ingredientUids.size==0){
    return Array()
  } else {
    val sn = NonEmptyList.fromListUnsafe(ingredientUids.toList)
    searchQuery(sn).query[MRecipeData].to[Array].transact(xa).unsafeRunSync()
  }
}

def getIngredientsData(): Array[MIngredientData] = {
  sql"SELECT id, name, uuid FROM ingredients".query[MIngredientData].to[Array].transact(xa).unsafeRunSync()
}

def getIngredients(): Array[Ingredient] = getIngredientsData().map(getMIngredientFromData).map(_.element)