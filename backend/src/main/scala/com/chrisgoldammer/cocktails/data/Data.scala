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
import com.chrisgoldammer.cocktails.data.types.*
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
  getIngredientsSetsStoredQuery.query[StoredElement[FullIngredientSet]].to[List].transact(xa).unsafeRunSync()
}

def getIngredientSets() = getIngredientSetsStored().map(_.element)

object ItemType extends Enumeration {
  type ItemType = Value
  val RECIPE, INGREDIENT = Value
}

def getIngredientsData(): List[MFullIngredientData] = {
  getIngredientsQuery.query[MFullIngredientData].to[List].transact(xa).unsafeRunSync()
}

def setup(): Unit = {
  dropTables.transact(xa).unsafeRunSync()
  createTables.transact(xa).unsafeRunSync()
  insertFromSetupData(setupDataSimple)
}



