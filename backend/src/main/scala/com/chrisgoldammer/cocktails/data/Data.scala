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
import com.chrisgoldammer.cocktails.data.*


val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", // driver classname
  connString,
  "postgres", // user
  "" // password
)


def getUuid() = randomUUID().toString

val defaultPort = "jdbc:postgresql://localhost:5432/world"
val connString = sys.env.get("POSTGRESPORT").getOrElse(defaultPort)


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
def getSetByName(sIngredientSets: List[StoredElement[IngredientSet]], name: String): StoredElement[IngredientSet] = sIngredientSets.groupBy(_.element.name).transform((k, v) => v.head)(name)

case class SetupData(ingredientData: List[IngredientDataRaw], recipeNames: Map[String, List[String]], ingredientSets: Map[String, List[String]])

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

  val mIngredientSets = for {
    setName <- sd.ingredientSets.keys
  } yield insertIngredientSet(setName).transact(xa).unsafeRunSync()


  val mIngredientSetIngredients: Iterable[StoredElement[IngredientSetIngredient]] = for {
    (setName, setIngredientNames) <- sd.ingredientSets
    ingredientName <- setIngredientNames
  } yield {
    val setId = getSetByName(mIngredientSets.toList, setName).id
    val ingredientId = getIngredientByName(mIngredients, ingredientName).id
    insertIngredientSetIngredient(setId = setId, ingredientId = ingredientId).transact(xa).unsafeRunSync()
  }
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

def getIngredientSetsStored(): List[StoredElement[FullIngredientSet]] = {
  sql"""
  with base AS (
    SELECT s.id, s.name, s.uuid, i.uuid AS ingredient_uuid
    FROM ingredient_sets s
    JOIN ingredient_set_ingredients isi ON s.id=isi.ingredient_set_id
    JOIN ingredients i ON isi.ingredient_id=i.id)
  SELECT id, min(name) as name, min(uuid) as uuid, array_agg(ingredient_uuid) as ingredient_uuids
  FROM base group by id
  """.query[StoredElement[FullIngredientSet]].to[List].transact(xa).unsafeRunSync()
}

def getIngredientSets() = getIngredientSetsStored().map(_.element)

object ItemType extends Enumeration {
  type ItemType = Value
  val RECIPE, INGREDIENT = Value
}

case class IngredientDataRaw(name: String, IngredientTagNames: List[String])

def setup(): Unit = {
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
  val sdSimple = SetupData(ingredientData = ingredientData, recipeNames = recipeNames, ingredientSets = ingredientSets)

  dropTables.transact(xa).unsafeRunSync()
  createTables.transact(xa).unsafeRunSync()
  insertFromSetupData(sdSimple)
}

def getIngredientsData(): List[MFullIngredientData] = {
  getIngredientsQuery.query[MFullIngredientData].to[List].transact(xa).unsafeRunSync()
}

