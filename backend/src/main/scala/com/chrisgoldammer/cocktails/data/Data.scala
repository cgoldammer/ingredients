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

val defaultPort = "jdbc:postgresql://localhost:5432/world"
val connString = sys.env.get("POSTGRESPORT").getOrElse(defaultPort)

val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", // driver classname
  connString,
  "postgres", // user
  "" // password
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

val createTags =
  sql"""
CREATE TABLE tags (
id SERIAL,
name VARCHAR NOT NULL UNIQUE,
PRIMARY KEY(id)
)
""".stripMargin

val createIngredientTags =
  sql"""
CREATE TABLE ingredient_tags (
id SERIAL,
ingredient_id INT,
tag_id INT,
CONSTRAINT fk_it_i FOREIGN KEY(ingredient_id) REFERENCES ingredients(id),
CONSTRAINT fk_it_t FOREIGN KEY(tag_id) REFERENCES tags(id),
PRIMARY KEY(id)
)
""".stripMargin

case class MElement[T](id: Int, element: T)

case class Ingredient(name: String, uuid: String)

case class Recipe(name: String, uuid: String)

case class RecipeIngredient()

case class FullRecipe(name: String, uuid: String, ingredients: Array[Ingredient])

case class RecipeIngredientData(name: String, uuid: String, ingredientName: String, ingredientUuid: String)

case class MIngredient(id: Int, element: Ingredient)

case class Tag(name: String)

case class IngredientTag()

case class MTag(id: Int, element: Tag)

case class MIngredientTag(id: Int, element: Tag)

case class MIngredientData(id: Int, name: String, uuid: String)

case class MRecipeIngredient(id: Int, element: RecipeIngredient)

case class MRecipeIngredientData(id: Int, recipeId: Int, ingredientId: Int)

case class MRecipe(id: Int, element: Recipe)

case class MRecipeData(id: Int, name: String, uuid: String)

case class MFullRecipe(id: Int, uuid: String, name: String, ingredients: Array[MIngredient])

case class IngredientResult(ingredients: Array[Ingredient])

sealed case class IngredientSearchList(ingredients: Array[String])


def getMIngredientFromData(md: MIngredientData): MIngredient = {
  val ingredient = Ingredient(name = md.name, uuid = md.uuid)
  MIngredient(id = md.id, element = ingredient)
}

def getMRecipeFromData(md: MRecipeData): MRecipe = {
  val recipe = Recipe(name = md.name, uuid = md.uuid)
  MRecipe(id = md.id, element = recipe)
}

def insertTag(name: String): ConnectionIO[MTag] = {
  sql"INSERT INTO tags (name) values ($name)".update.withUniqueGeneratedKeys("id", "name")
}


def insertIngredientTag(ingredientId: Int, tagId: Int): ConnectionIO[MIngredientTag] = {
  sql"INSERT INTO ingredient_tags (ingredient_id, tag_id) values ($ingredientId, $tagId)".update.withUniqueGeneratedKeys("id", "ingredient_id", "tag_id")
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
    case RecipeIngredientData(_, _, ingredientName, ingredientUuid) => Ingredient(ingredientName, ingredientUuid)
  }
}

val recipeIngredientDataQuery =
  sql"""
       |SELECT r.name, r.uuid, i.name AS ingredient_name, i.uuid AS ingredient_uuid
FROM recipe_ingredients ri
JOIN recipes r ON ri.recipe_id=r.id
JOIN ingredients i ON ri.ingredient_id=i.id
""".stripMargin

def createFullRecipes(rid: Array[RecipeIngredientData]): Array[FullRecipe] = {
  val grouped = rid.groupBy(_.name)
  grouped.toArray.map { case (name, ri) => FullRecipe(name, ri(0).uuid, ri.map(recipeIngredientToIngredient)) }
}

def getFullRecipes(): Array[FullRecipe] = {
  val rid = recipeIngredientDataQuery.query[RecipeIngredientData].to[Array].transact(xa).unsafeRunSync()
  return createFullRecipes(rid)
}

def dropTables(): Unit = {
  sql"DROP TABLE IF EXISTS ingredient_tags".update.run.transact(xa).unsafeRunSync()
  sql"DROP TABLE IF EXISTS tags".update.run.transact(xa).unsafeRunSync()
  sql"DROP TABLE IF EXISTS recipe_ingredients".update.run.transact(xa).unsafeRunSync()
  sql"DROP TABLE IF EXISTS recipes".update.run.transact(xa).unsafeRunSync()
  sql"DROP TABLE IF EXISTS ingredients".update.run.transact(xa).unsafeRunSync()
}

def createTables(): Unit = {
  createRecipe.update.run.transact(xa).unsafeRunSync()
  createIngredients.update.run.transact(xa).unsafeRunSync()
  createRecipeIngredients.update.run.transact(xa).unsafeRunSync()
  createTags.update.run.transact(xa).unsafeRunSync()
  createIngredientTags.update.run.transact(xa).unsafeRunSync()
}

def getRecipeByName(mRecipes: Array[MRecipeData], name: String): MRecipeData = mRecipes.groupBy(_.name).transform((k, v) => v.head)(name)
def getIngredientByName(mIngredients: Array[MIngredient], name: String): MIngredient = mIngredients.groupBy(_.element.name).transform((k, v) => v.head)(name)
def getTagByName(mTags: Array[MTag], name: String): MTag = mTags.groupBy(_.element.name).transform((k, v) => v.head)(name)

case class SetupData(ingredientData: Array[IngredientDataRaw], recipeNames: Map[String, Array[String]])

def insertFromSetupData(sd: SetupData): Unit = {

  val tagNames: Array[String] =
    for {
      ingredient <- sd.ingredientData
      tag <- ingredient.IngredientTagNames
    } yield tag

  val mTags =
    for (tagName <- tagNames.distinct)
      yield insertTag(tagName).transact(xa).unsafeRunSync()


  val mIngredients =
    for (ingredient <- sd.ingredientData)
      yield insertIngredient(ingredient.name).transact(xa).unsafeRunSync()

  val x =
    for {
      ingredient <- sd.ingredientData
      tagName <- ingredient.IngredientTagNames
    } yield {
      val tagId = getTagByName(mTags, tagName).id
      val ingredientId = getIngredientByName(mIngredients, ingredient.name).id
      insertIngredientTag(ingredientId = ingredientId, tagId = tagId).transact(xa).unsafeRunSync()
    }

  val mRecipes: Iterable[MRecipeData] =
    for (recipe <- sd.recipeNames.keys)
      yield insertRecipe(recipe).transact(xa).unsafeRunSync()

  val mRecipeIngredients = for {
    (recipeName, recipeIngredientNames) <- sd.recipeNames
    recipeIngredientName <- recipeIngredientNames.toList
  } yield {
    val recipeId = getRecipeByName(mRecipes.toArray, recipeName).id
    val ingredientId = getIngredientByName(mIngredients, recipeIngredientName).id
    insertRecipeIngredient(recipeId = recipeId, ingredientId = ingredientId).transact(xa).unsafeRunSync()
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
    """ ++ inFragment ++
    fr"""
    ),
    recipeNumberFound AS (
      SELECT recipe_id, SUM(1) AS num_ing_found
      FROM candidates
      GROUP BY recipe_id
    ),
    recipeNumberIngredients AS (
      SELECT recipe_id, SUM(1) AS num_ing_total
      FROM recipe_ingredients
      GROUP BY recipe_id
    )
    SELECT r.id, name, uuid
    FROM recipes r
    JOIN recipeNumberIngredients rni
    ON r.id=rni.recipe_id
    JOIN recipeNumberFound rnf
    ON r.id=rnf.recipe_id
    WHERE num_ing_found=num_ing_total
    """
}

def getMRecipesForIngredients(ingredientUids: Array[String]): Array[MRecipeData] = {
  if (ingredientUids.size == 0) {
    return Array()
  } else {
    val sn = NonEmptyList.fromListUnsafe(ingredientUids.toList)
    println(searchQuery(sn))
    searchQuery(sn).query[MRecipeData].to[Array].transact(xa).unsafeRunSync()
  }
}

def recipeFromMRecipe(mRecipe: MRecipeData): Recipe = Recipe(name = mRecipe.name, uuid = mRecipe.uuid)

def getRecipesForIngredients(ingredientUids: Array[String]): Array[Recipe] = {
  getMRecipesForIngredients(ingredientUids).map(recipeFromMRecipe)
}

def getIngredientsData(): Array[MIngredientData] = {
  sql"SELECT id, name, uuid FROM ingredients".query[MIngredientData].to[Array].transact(xa).unsafeRunSync()
}

def getIngredients(): Array[Ingredient] = getIngredientsData().map(getMIngredientFromData).map(_.element)

val ingredientTagNames = Array("Sugar", "Liquor", "Fortified Wine", "Bitter", "Strong", "Other")


object ItemType extends Enumeration {
  type ItemType = Value
  val RECIPE, INGREDIENT = Value
}
//
//case class IngredientTagData(name: String, itemType: ItemType)
//
//val ingredientTags = ingredientTagNames.map(n => IngredientTagData(name = n, itemType = ItemType.INGREDIENT))

case class IngredientDataRaw(name: String, IngredientTagNames: Array[String])

def setup(): Unit = {
  val ingredientData: Array[IngredientDataRaw] = Array(
    IngredientDataRaw("Bourbon", Array("Strong")),
    IngredientDataRaw("Dry Vermouth", Array("Fortified Wine")),
    IngredientDataRaw("Campari", Array("Other")),
    IngredientDataRaw("Sugar", Array("Sugar")),
    IngredientDataRaw("Bitters", Array("Bitter")),
  )
  val recipeNames = Map(
    "Boulevardier" -> Array("Bourbon", "Dry Vermouth", "Campari"),
    "Old Fashioned" -> Array("Bourbon", "Sugar", "Bitters")
  )
  val sdSimple = SetupData(ingredientData = ingredientData, recipeNames = recipeNames)

  dropTables()
  createTables()
  insertFromSetupData(sdSimple)
}

object CallMe {
  def main(args: Array[String]): Unit = {
    println("Setup starting")
    setup()
    println("Setup complete")
  }
}