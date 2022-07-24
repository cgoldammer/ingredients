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
import doobie.postgres.*
import doobie.postgres.implicits.*
import com.chrisgoldammer.cocktails.queries.*


def getUuid() = randomUUID().toString

val defaultPort = "jdbc:postgresql://localhost:5432/world"
val connString = sys.env.get("POSTGRESPORT").getOrElse(defaultPort)

val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", // driver classname
  connString,
  "postgres", // user
  "" // password
)


case class MElement[T](id: Int, element: T)

case class Ingredient(name: String, uuid: String)

case class FullIngredient(name: String, uuid: String, tags: List[Tag])

case class Recipe(name: String, uuid: String)

case class RecipeIngredient()

case class FullRecipe(name: String, uuid: String, ingredients: List[Ingredient])

case class RecipeIngredientData(name: String, uuid: String, ingredientName: String, ingredientUuid: String)

case class MFullIngredient(id: Int, element: FullIngredient)

case class Tag(name: String)

case class IngredientTag()

case class MTagData(id: Int, name: String)

case class MTag(id: Int, element: Tag)

case class MIngredient(id: Int, element: Ingredient)

case class MIngredientTag(id: Int, element: Tag)

case class MFullIngredientData(id: Int, name: String, uuid: String, tags: List[String])

case class MRecipeIngredient(id: Int, element: RecipeIngredient)

case class MRecipeIngredientData(id: Int, recipeId: Int, ingredientId: Int)

case class MRecipe(id: Int, element: Recipe)

case class MRecipeData(id: Int, name: String, uuid: String)

case class MFullRecipe(id: Int, uuid: String, name: String, ingredients: List[MIngredient])

case class IngredientResult(ingredients: List[Ingredient])

sealed case class IngredientSearchList(ingredients: List[String])


def getMFullIngredientFromData(md: MFullIngredientData): MFullIngredient = {
  val tags = md.tags.map(t => Tag(t))
  val ingredient = FullIngredient(name = md.name, uuid = md.uuid, tags = tags)
  MFullIngredient(id = md.id, element = ingredient)
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

def createFullRecipes(rid: List[RecipeIngredientData]): List[FullRecipe] = {
  val grouped = rid.groupBy(_.name)
  grouped.toList.map { case (name, ri) => FullRecipe(name, ri(0).uuid, ri.map(recipeIngredientToIngredient)) }
}

def getFullRecipes(): List[FullRecipe] = {
  val rid = recipeIngredientDataQuery.query[RecipeIngredientData].to[List].transact(xa).unsafeRunSync()
  return createFullRecipes(rid)
}


def getRecipeByName(mRecipes: List[MRecipeData], name: String): MRecipeData = mRecipes.groupBy(_.name).transform((k, v) => v.head)(name)
def getIngredientByName(mIngredients: List[MIngredient], name: String): MIngredient = mIngredients.groupBy(_.element.name).transform((k, v) => v.head)(name)
def getTagByName(mTags: List[MTag], name: String): MTag = mTags.groupBy(_.element.name).transform((k, v) => v.head)(name)

case class SetupData(ingredientData: List[IngredientDataRaw], recipeNames: Map[String, List[String]])

def insertFromSetupData(sd: SetupData): Unit = {

  val tagNames: List[String] =
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
    val recipeId = getRecipeByName(mRecipes.toList, recipeName).id
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

def getMRecipesForIngredients(ingredientUids: List[String]): List[MRecipeData] = {
  if (ingredientUids.size == 0) {
    return List()
  } else {
    val sn = NonEmptyList.fromListUnsafe(ingredientUids.toList)
    println(searchQuery(sn))
    searchQuery(sn).query[MRecipeData].to[List].transact(xa).unsafeRunSync()
  }
}

def recipeFromMRecipe(mRecipe: MRecipeData): Recipe = Recipe(name = mRecipe.name, uuid = mRecipe.uuid)

def getRecipesForIngredients(ingredientUids: List[String]): List[Recipe] = {
  getMRecipesForIngredients(ingredientUids).map(recipeFromMRecipe)
}

def getIngredientsData(): List[MFullIngredientData] = {
  sql"""
  with base AS (SELECT i.id, i.name, i.uuid, t.name AS tag_name
              FROM ingredients i
                       JOIN ingredient_tags it on i.id = it.ingredient_id
                       JOIN tags t ON it.tag_id = t.id)
SELECT id, min(name) as name, min(uuid) as uuid, List_agg(tag_name) as tags FROM base
                 group by id
  """.query[MFullIngredientData].to[List].transact(xa).unsafeRunSync()
}

def getIngredients(): List[FullIngredient] = getIngredientsData().map(getMFullIngredientFromData).map(_.element)

def getTagsData(): List[MTagData] = {
  sql"SELECT id, name FROM tags".query[MTagData].to[List].transact(xa).unsafeRunSync()
}

def getTagFromMTag(mt: MTagData): Tag = Tag(mt.name)

def getTags(): List[Tag] = getTagsData().map(getTagFromMTag)

object ItemType extends Enumeration {
  type ItemType = Value
  val RECIPE, INGREDIENT = Value
}

case class IngredientDataRaw(name: String, IngredientTagNames: List[String])

def setup(): Unit = {
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
  val sdSimple = SetupData(ingredientData = ingredientData, recipeNames = recipeNames)

  dropTables.transact(xa).unsafeRunSync()
  createTables.transact(xa).unsafeRunSync()
  insertFromSetupData(sdSimple)
}

object CallMe {
  def main(args: List[String]): Unit = {
    println("Setup starting")
    setup()
    println("Setup complete")
  }
}