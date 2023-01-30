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
import com.chrisgoldammer.cocktails.data.types.*
import com.chrisgoldammer.cocktails.data.queries.*

def camel2underscores(x: String) = {
  "_?[A-Z][a-z\\d]+".r
    .findAllMatchIn(x)
    .map(_.group(0).toLowerCase)
    .mkString("_")
}

def getUuid = randomUUID().toString

def recipeIngredientToIngredient(ri: FullRecipeData): Ingredient = {
  ri match {
    case FullRecipeData(_, _, _, ingredientName, ingredientUuid) =>
      Ingredient(ingredientName, ingredientUuid)
  }
}

def createFullRecipes(rid: List[FullRecipeData]): List[FullRecipe] = {
  val grouped = rid.groupBy(_.name)
  grouped.toList.map { case (name, ri) =>
    FullRecipe(
      name,
      ri.head.uuid,
      ri.head.description,
      ri.map(recipeIngredientToIngredient)
    )
  }
}

def getFullRecipesIO: ConnectionIO[List[FullRecipe]] =
  recipeIngredientDataQuery
    .query[FullRecipeData]
    .to[List]
    .map(createFullRecipes)

def getByProperty[T, S](storedList: List[StoredElement[T]], getProperty: T => S): S => StoredElement[T] =
  uniqueVal => storedList.groupBy(se => getProperty(se.element)).transform((_, v) => v.head)(uniqueVal)

def getRecipesForIngredientsIO(
    ingredientUids: List[String]
): ConnectionIO[List[FullRecipe]] =
  NonEmptyList
    .fromList(ingredientUids)
    .map(sn =>
      searchQuery(sn)
        .query[FullRecipeData]
        .to[List]
        .map(createFullRecipes)
    )
    .getOrElse(List().pure[ConnectionIO])

def getMFullIngredientFromData(
    md: MFullIngredientData
): StoredElement[FullIngredient] = {
  val tags = md.tags.map(t => Tag(t))
  val ingredient = FullIngredient(name = md.name, uuid = md.uuid, tags = tags, numberRecipes=md.numberRecipes)
  StoredElement(md.id, ingredient)
}

def getIngredientsIO: ConnectionIO[List[FullIngredient]] = for {
  ing <- getIngredientsDataIO
} yield ing.map(getMFullIngredientFromData).map(_.element)

def getTagsDataIO: ConnectionIO[List[StoredElement[Tag]]] = {
  sql"SELECT id, name FROM tags".query[StoredElement[Tag]].to[List]
}

def getTagsIO: ConnectionIO[List[Tag]] = for {
  tags <- getTagsDataIO
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

def getIngredientsDataIO: ConnectionIO[List[MFullIngredientData]] =
  getIngredientsQuery.query[MFullIngredientData].to[List]

def setupIO(sdo: Option[SetupData]): ConnectionIO[Unit] = {
  val inserter = sdo match {
    case Some(sd) => insertFromSetupDataIO(sd, doobieBackingStore())
    case None => ().pure[ConnectionIO]
  }
  dropTables >> createTables >> inserter
}

def insertFromSetupDataIO(
    sd: SetupData,
    bStore: BackingStore
): ConnectionIO[Unit] = for {

  users <- sd.users.traverse(bStore.put)

  mTags <- sd.ingredientData
    .flatMap(_.IngredientTagNames)
    .distinct
    .traverse(insertTag)
  tagGetter = getByProperty(mTags, _.name)
  mIngredients <- sd.ingredientData.traverse(ingredient =>
    insertIngredient(ingredient.name)
  )
  ingredientGetter = getByProperty(mIngredients, _.name)
  ids = for {
    ingredient <- sd.ingredientData
    tagName <- ingredient.IngredientTagNames
  } yield {
    val tagId = tagGetter(tagName).id
    val ingredientId: Int =
      ingredientGetter(ingredient.name).id
    (ingredientId, tagId)
  }

  _ <- ids.traverse{case (ingredientId: Int, tagId: Int) => insertIngredientTag(ingredientId, tagId)}

  mRecipes <- sd.recipeData.traverse((rd: RecipeData) =>
    insertRecipe(rd.name, rd.description)
  )
  recipeGetter = getByProperty(mRecipes, _.name)

  ids: List[(Int, Int)] = for {
    rd <- sd.recipeData
    recipeIngredientName <- rd.ingredients
  } yield {
    val recipeId = recipeGetter(rd.name).id
    val ingredientId =
      ingredientGetter(recipeIngredientName).id
    (recipeId, ingredientId)
  }

  _ <- ids.traverse{case (recipeId: Int, ingredientId: Int) => insertRecipeIngredient(recipeId, ingredientId)}

  mIngredientSets <- sd.ingredientSets.keys.toList.traverse(s =>
    insertIngredientSet(s, users.head.get.id)
  )
  setGetter = getByProperty(mIngredientSets, _.name)

  ids = for {
    (setName, setIngredientNames) <- sd.ingredientSets.toList
    ingredientName <- setIngredientNames
  } yield
    val setId = setGetter(setName).id
    val ingredientId = ingredientGetter(ingredientName).id
    (setId, ingredientId)
  _ <- ids.traverse((setId: Int, ingredientId: Int) =>
    insertIngredientSetIngredient(setId, ingredientId)
  )
} yield None

class DataTools(dbSetup: DBSetup):
  val xa: Transactor[IO] = getTransactor(dbSetup)
  def transact[A](c: ConnectionIO[A]): IO[A] = c.transact(xa)

object DataTools {}
