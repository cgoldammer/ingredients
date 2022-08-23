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

case class StoredElement[T](id: Int, element: T)

case class Ingredient(name: String, uuid: String)
implicit val sReadIngredient: Read[StoredElement[Ingredient]] =
  Read[(Int, String, String)].map { case (id, name, uuid) => new StoredElement(id, Ingredient(name, uuid))}


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
case class MFullIngredientData(id: Int, name: String, uuid: String, tags: List[String])
case class MFullRecipe(id: Int, uuid: String, name: String, ingredients: List[StoredElement[Ingredient]])

case class Results[T](data: List[T], name: String = "Default")
// case class IngredientResult(ingredients: List[Ingredient])
// case class IngredientSearchList(ingredients: List[String])




def insertIngredientTag(ingredientId: Int, tagId: Int): ConnectionIO[StoredElement[IngredientTag]] = {
  sql"INSERT INTO ingredient_tags (ingredient_id, tag_id) values ($ingredientId, $tagId)".update.withUniqueGeneratedKeys("id", "ingredient_id", "tag_id")
}

def insertTag(name: String): ConnectionIO[StoredElement[Tag]] = {
  sql"INSERT INTO tags (name) values ($name)".update.withUniqueGeneratedKeys("id", "name")
}

def insertIngredient(name: String): ConnectionIO[StoredElement[Ingredient]] = {
  val uuid = getUuid()
  sql"INSERT INTO ingredients (name, uuid) values ($name, $uuid)".update.withUniqueGeneratedKeys("id", "name", "uuid")
}

def insertRecipeIngredient(recipeId: Int, ingredientId: Int): ConnectionIO[StoredElement[RecipeIngredient]] = {
  sql"INSERT INTO recipe_ingredients (recipe_id, ingredient_id) values ($recipeId, $ingredientId)"
    .update.withUniqueGeneratedKeys("id", "recipe_id", "ingredient_id")
}

def insertRecipe(name: String): ConnectionIO[StoredElement[Recipe]] = {
  val uuid = getUuid()
  sql"INSERT INTO recipes (name, uuid) values ($name, $uuid)".update.withUniqueGeneratedKeys("id", "name", "uuid")
}


val recipeIngredientDataQuery =
  sql"""
       |SELECT r.name, r.uuid, i.name AS ingredient_name, i.uuid AS ingredient_uuid
FROM recipe_ingredients ri
JOIN recipes r ON ri.recipe_id=r.id
JOIN ingredients i ON ri.ingredient_id=i.id
""".stripMargin

def recipeIngredientToIngredient(ri: FullRecipeData): Ingredient = {
  ri match {
    case FullRecipeData(_, _, ingredientName, ingredientUuid) => Ingredient(ingredientName, ingredientUuid)
  }
}

def createFullRecipes(rid: List[FullRecipeData]): List[FullRecipe] = {
  val grouped = rid.groupBy(_.name)
  grouped.toList.map { case (name, ri) => FullRecipe(name, ri(0).uuid, ri.map(recipeIngredientToIngredient)) }
}

def getFullRecipes(): List[FullRecipe] = {
  val rid = recipeIngredientDataQuery.query[FullRecipeData].to[List].transact(xa).unsafeRunSync()
  return createFullRecipes(rid)
}

def getRecipeByName(sRecipes: List[StoredElement[Recipe]], name: String): StoredElement[Recipe] = sRecipes.groupBy(_.element.name).transform((k, v) => v.head)(name)
def getTagByName(sTags: List[StoredElement[Tag]], name: String): StoredElement[Tag] = sTags.groupBy(_.element.name).transform((k, v) => v.head)(name)
def getIngredientByName(sIngredients: List[StoredElement[Ingredient]], name: String): StoredElement[Ingredient] = sIngredients.groupBy(_.element.name).transform((k, v) => v.head)(name)

case class SetupData(ingredientData: List[IngredientDataRaw], recipeNames: Map[String, List[String]], ingredientSets: List[IngredientSetRaw])

def insertFromSetupData(sd: SetupData): Unit = {

  val tagNames: List[String] =
    for {
      ingredient <- sd.ingredientData
      tag <- ingredient.IngredientTagNames
    } yield tag

  val mTags: Iterable[StoredElement[Tag]] =
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
      val tagId = getTagByName(mTags.toList, tagName).id
      val ingredientId = getIngredientByName(mIngredients, ingredient.name).id
      insertIngredientTag(ingredientId, tagId).transact(xa).unsafeRunSync()
    }

  val mRecipes: Iterable[StoredElement[Recipe]] =
    for (recipe <- sd.recipeNames.keys)
      yield insertRecipe(recipe).transact(xa).unsafeRunSync()

  val mRecipeIngredients: Iterable[StoredElement[RecipeIngredient]] = for {
    (recipeName, recipeIngredientNames) <- sd.recipeNames
    recipeIngredientName <- recipeIngredientNames
  } yield {
    val recipeId = getRecipeByName(mRecipes.toList, recipeName).id
    val ingredientId = getIngredientByName(mIngredients, recipeIngredientName).id
    insertRecipeIngredient(recipeId = recipeId, ingredientId = ingredientId).transact(xa).unsafeRunSync()
  }

  /*

	val mIngredientSets = for {
    setName <- sd.ingredientSets.map(_.name)
  } yield insertIngredientSet
  */

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

def getMRecipesForIngredients(ingredientUids: List[String]): List[StoredElement[Recipe]] = {
  if (ingredientUids.size == 0) {
    return List()
  } else {
    val sn = NonEmptyList.fromListUnsafe(ingredientUids.toList)
    searchQuery(sn).query[StoredElement[Recipe]].to[List].transact(xa).unsafeRunSync()
  }
}

def getRecipesForIngredients(ingredientUids: List[String]): List[Recipe] = getMRecipesForIngredients(ingredientUids).map(_.element)

def getIngredientsData(): List[MFullIngredientData] = {
  sql"""
  with base AS (SELECT i.id, i.name, i.uuid, t.name AS tag_name
              FROM ingredients i
                       JOIN ingredient_tags it on i.id = it.ingredient_id
                       JOIN tags t ON it.tag_id = t.id)
SELECT id, min(name) as name, min(uuid) as uuid, array_agg(tag_name) as tags FROM base
                 group by id
  """.query[MFullIngredientData].to[List].transact(xa).unsafeRunSync()
}

def getMFullIngredientFromData(md: MFullIngredientData): StoredElement[FullIngredient] = {
  val tags = md.tags.map(t => Tag(t))
  val ingredient = FullIngredient(name = md.name, uuid = md.uuid, tags = tags)
  StoredElement(md.id, ingredient)
}

def getIngredients(): List[FullIngredient] = getIngredientsData().map(getMFullIngredientFromData).map(_.element)

def getTagsData(): List[StoredElement[Tag]] = {
  sql"SELECT id, name FROM tags".query[StoredElement[Tag]].to[List].transact(xa).unsafeRunSync()
}

def getTags(): List[Tag] = getTagsData().map(_.element)

object ItemType extends Enumeration {
  type ItemType = Value
  val RECIPE, INGREDIENT = Value
}

case class IngredientDataRaw(name: String, IngredientTagNames: List[String])

case class IngredientSetRaw(name: String, IngredientNames: List[String])

def setup(): Unit = {
  val homeIngredients = List("Gin", "Vodka", "Bourbon", "Orange Liquour",
    "Scotch", "Aperol", "Dry Vermouth", "Campari", "Sugar", "Bitters", "Egg White")
  val ingredientSets: List[IngredientSetRaw] = List(
    IngredientSetRaw("home", homeIngredients)
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
  val sdSimple = SetupData(ingredientData = ingredientData, recipeNames = recipeNames, ingredientSets = ingredientSets)

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
