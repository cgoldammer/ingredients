package com.chrisgoldammer.cocktails.data

import java.util.UUID.randomUUID

import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.catsSyntaxFlatMapOps
import cats.implicits.toTraverseOps
import doobie.ConnectionIO
import doobie.Transactor
import doobie.implicits.toConnectionIOOps
import doobie.implicits.toSqlInterpolator
import doobie.postgres.implicits.*
import doobie.util.ExecutionContexts

import com.chrisgoldammer.cocktails.cryptocore.*
import com.chrisgoldammer.cocktails.data.*
import com.chrisgoldammer.cocktails.data.types.*

def camel2underscores(x: String) = {
  "_?[A-Z][a-z\\d]+".r
    .findAllMatchIn(x)
    .map(_.group(0).toLowerCase)
    .mkString("_")
}

def getUuid() = randomUUID().toString

def recipeIngredientToIngredient(ri: FullRecipeData): Ingredient = {
  ri match {
    case FullRecipeData(_, _, ingredientName, ingredientUuid) =>
      Ingredient(ingredientName, ingredientUuid)
  }
}

def createFullRecipes(rid: List[FullRecipeData]): List[FullRecipe] = {
  val grouped = rid.groupBy(_.name)
  grouped.toList.map { case (name, ri) =>
    FullRecipe(name, ri(0).uuid, ri.map(recipeIngredientToIngredient))
  }
}

def getFullRecipesIO(): ConnectionIO[List[FullRecipe]] =
  recipeIngredientDataQuery
    .query[FullRecipeData]
    .to[List]
    .map(createFullRecipes)

def getRecipeByName(
    sRecipes: List[StoredElement[Recipe]],
    name: String
): StoredElement[Recipe] =
  sRecipes.groupBy(_.element.name).transform((k, v) => v.head)(name)
def getTagByName(
    sTags: List[StoredElement[Tag]],
    name: String
): StoredElement[Tag] =
  sTags.groupBy(_.element.name).transform((k, v) => v.head)(name)
def getIngredientByName(
    sIngredients: List[StoredElement[Ingredient]],
    name: String
): StoredElement[Ingredient] =
  sIngredients.groupBy(_.element.name).transform((k, v) => v.head)(name)
def getSetByName(
    sIngredientSets: List[StoredElement[IngredientSet]],
    name: String
): StoredElement[IngredientSet] =
  sIngredientSets.groupBy(_.element.name).transform((k, v) => v.head)(name)

def getMRecipesForIngredientsIO(
    ingredientUids: List[String]
): ConnectionIO[List[StoredElement[Recipe]]] =
  NonEmptyList
    .fromList(ingredientUids)
    .map(sn => searchQuery(sn).query[StoredElement[Recipe]].to[List])
    .getOrElse(List().pure[ConnectionIO])

def getRecipesForIngredientsIO(
    ingredientUids: List[String]
): ConnectionIO[List[Recipe]] = for {
  m <- getMRecipesForIngredientsIO(ingredientUids)
} yield m.map(_.element)

def getMFullIngredientFromData(
    md: MFullIngredientData
): StoredElement[FullIngredient] = {
  val tags = md.tags.map(t => Tag(t))
  val ingredient = FullIngredient(name = md.name, uuid = md.uuid, tags = tags)
  StoredElement(md.id, ingredient)
}

def getIngredientsIO(): ConnectionIO[List[FullIngredient]] = for {
  ing <- getIngredientsDataIO()
} yield ing.map(getMFullIngredientFromData).map(_.element)

def getTagsDataIO(): ConnectionIO[List[StoredElement[Tag]]] = {
  sql"SELECT id, name FROM tags".query[StoredElement[Tag]].to[List]
}

def getTagsIO(): ConnectionIO[List[Tag]] = for {
  tags <- getTagsDataIO()
} yield tags.map(_.element)

def getIngredientSetsStoredIO(
    userUuid: String
): ConnectionIO[List[StoredElement[FullIngredientSet]]] =
  getIngredientsSetsStoredQuery(userUuid)
    .query[StoredElement[FullIngredientSet]]
    .to[List]

def getIngredientSetsIO(
    userUuid: String
): ConnectionIO[List[FullIngredientSet]] = for {
  is <- getIngredientSetsStoredIO(userUuid)
} yield is.map(_.element)

object ItemType extends Enumeration {
  type ItemType = Value
  val RECIPE, INGREDIENT = Value
}

def getIngredientsDataIO(): ConnectionIO[List[MFullIngredientData]] =
  getIngredientsQuery.query[MFullIngredientData].to[List]

def setupIO(sd: SetupData): ConnectionIO[Unit] =
  dropTables >> createTables >> insertFromSetupDataIO(sd, doobieBackingStore())

def insertFromSetupDataIO(
    sd: SetupData,
    bStore: BackingStore
): ConnectionIO[Unit] = for {

  users <- sd.users.traverse(bStore.put)

  mTags <- sd.ingredientData
    .flatMap(_.IngredientTagNames)
    .distinct
    .traverse(insertTag)
  mIngredients <- sd.ingredientData.traverse(ingredient =>
    insertIngredient(ingredient.name)
  )
  ids = for {
    ingredient <- sd.ingredientData
    tagName <- ingredient.IngredientTagNames
  } yield {
    val tagId = getTagByName(mTags.toList, tagName).id
    val ingredientId: Int =
      getIngredientByName(mIngredients, ingredient.name).id
    (ingredientId, tagId)
  }

  _ <- ids.traverse(x =>
    x match {
      case (ingredientId: Int, tagId: Int) =>
        insertIngredientTag(ingredientId, tagId)
    }
  )

  mRecipes <- sd.recipeData.traverse((rd: RecipeData) => insertRecipe(rd.name, rd.description))

  ids: List[(Int, Int)] = for {
    rd <- sd.recipeData
    recipeIngredientName <- rd.ingredients
  } yield {
    val recipeId = getRecipeByName(mRecipes.toList, rd.name).id
    val ingredientId =
      getIngredientByName(mIngredients, recipeIngredientName).id
    (recipeId, ingredientId)
  }

  mRecipeIngredients <- ids.traverse(x =>
    x match {
      case (recipeId: Int, ingredientId: Int) =>
        insertRecipeIngredient(recipeId, ingredientId)
    }
  )

  mIngredientSets <- sd.ingredientSets.keys.toList.traverse(s =>
    insertIngredientSet(s, users.head.get.id)
  )

  ids = for {
    (setName, setIngredientNames) <- sd.ingredientSets
    ingredientName <- setIngredientNames
  } yield {
    val setId = getSetByName(mIngredientSets.toList, setName).id
    val ingredientId = getIngredientByName(mIngredients, ingredientName).id
    (setId, ingredientId)
  }

  mIngredientSetIngredients <- ids.toList.traverse(x =>
    x match {
      case (setId: Int, ingredientId: Int) =>
        insertIngredientSetIngredient(setId, ingredientId)
    }
  )
} yield None

def saveIngredientSet(
    userId: Int,
    name: String,
    ingredientUuids: List[String]
): ConnectionIO[Unit] = {
  for {
    setCreated <- insertIngredientSet(name, userId)
    _ <- bulkInsertIngredientSetIngredient(setCreated.id, ingredientUuids)
  } yield None
}


def getTransactor(dbSetup: DBSetup): Transactor[IO] =
  Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    dbSetup.getConnString(),
    "postgres", // user
    "" // password
  )

class DataTools(dbSetup: DBSetup):
  val xa: Transactor[IO] = getTransactor(dbSetup)

  def getTags(): IO[List[Tag]] = getTagsIO().transact(xa)
  def setup(): IO[Unit] = setupIO(setupDataSimple).transact(xa)
  def getFullRecipes(): IO[List[FullRecipe]] = getFullRecipesIO().transact(xa)
  def getIngredients(): IO[List[FullIngredient]] =
    getIngredientsIO().transact(xa)
  def getRecipesForIngredients(ingredientUids: List[String]): IO[List[Recipe]] =
    getRecipesForIngredientsIO(ingredientUids).transact(xa)
  def getIngredientSets(userUuid: String): IO[List[FullIngredientSet]] =
    getIngredientSetsIO(userUuid).transact(xa)

  def saveIngredientSetIO(
      userId: Int,
      name: String,
      ingredientUuids: List[String]
  ): IO[Unit] = saveIngredientSet(userId, name, ingredientUuids).transact(xa)

object DataTools {}
