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


// val connstringServer = localhost:5432

/* 
 * I need a way of saying: Encapsulate anything
 * that accesses the DB as a function like
 * connector -> action
 * And when I instantiate the app, I can pass
 * it setup info (which includes the connection data)
 * And that sets up the right db
*/






def getUuid() = randomUUID().toString
val defaultPort = "jdbc:postgresql://localhost:5432/ingredients"
val connString = sys.env.get("POSTGRESPORT").getOrElse(defaultPort)

val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", // driver classname
  connString,
  "postgres", // user
  "" // password
)

def recipeIngredientToIngredient(ri: FullRecipeData): Ingredient = {
  ri match {
    case FullRecipeData(_, _, ingredientName, ingredientUuid) => Ingredient(ingredientName, ingredientUuid)
  }
}

def createFullRecipes(rid: List[FullRecipeData]): List[FullRecipe] = {
  val grouped = rid.groupBy(_.name)
  grouped.toList.map { case (name, ri) => FullRecipe(name, ri(0).uuid, ri.map(recipeIngredientToIngredient)) }
}

def getFullRecipesIO(): ConnectionIO[List[FullRecipe]] = recipeIngredientDataQuery.query[FullRecipeData].to[List].map(createFullRecipes)

def getRecipeByName(sRecipes: List[StoredElement[Recipe]], name: String): StoredElement[Recipe] = sRecipes.groupBy(_.element.name).transform((k, v) => v.head)(name)
def getTagByName(sTags: List[StoredElement[Tag]], name: String): StoredElement[Tag] = sTags.groupBy(_.element.name).transform((k, v) => v.head)(name)
def getIngredientByName(sIngredients: List[StoredElement[Ingredient]], name: String): StoredElement[Ingredient] = sIngredients.groupBy(_.element.name).transform((k, v) => v.head)(name)
def getSetByName(sIngredientSets: List[StoredElement[IngredientSet]], name: String): StoredElement[IngredientSet] = sIngredientSets.groupBy(_.element.name).transform((k, v) => v.head)(name)

def getMRecipesForIngredientsIO(ingredientUids: List[String]): ConnectionIO[List[StoredElement[Recipe]]] =
  NonEmptyList
    .fromList(ingredientUids)
    .map(sn => searchQuery(sn).query[StoredElement[Recipe]].to[List])
    .getOrElse(List().pure[ConnectionIO])

def getRecipesForIngredientsIO(ingredientUids: List[String]): ConnectionIO[List[Recipe]] = for {
  m <- getMRecipesForIngredientsIO(ingredientUids)
} yield m.map(_.element)

def getMFullIngredientFromData(md: MFullIngredientData): StoredElement[FullIngredient] = {
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

def getIngredientSetsStoredIO(): ConnectionIO[List[StoredElement[FullIngredientSet]]] = getIngredientsSetsStoredQuery.query[StoredElement[FullIngredientSet]].to[List]

def getIngredientSetsIO(): ConnectionIO[List[FullIngredientSet]] = for {
  is <- getIngredientSetsStoredIO()
} yield is.map(_.element)

object ItemType extends Enumeration {
  type ItemType = Value
  val RECIPE, INGREDIENT = Value
}

def getIngredientsDataIO(): ConnectionIO[List[MFullIngredientData]] = getIngredientsQuery.query[MFullIngredientData].to[List]

case class DBSetup()


def setupIO(sd: SetupData): ConnectionIO[Unit] = dropTables >> createTables >> insertFromSetupDataIO(sd)


def insertFromSetupDataIO(sd: SetupData): ConnectionIO[Unit] = for {
  mTags <- sd.ingredientData.flatMap(_.IngredientTagNames).distinct.traverse(insertTag)
  mIngredients <- sd.ingredientData.traverse(ingredient => insertIngredient(ingredient.name))
  ids = for {
    ingredient <- sd.ingredientData
    tagName <- ingredient.IngredientTagNames
  } yield {
    val tagId = getTagByName(mTags.toList, tagName).id
    val ingredientId: Int = getIngredientByName(mIngredients, ingredient.name).id
    (ingredientId, tagId)
  }

  _ <- ids.traverse(x => x match {
      case (ingredientId: Int, tagId:Int) => insertIngredientTag(ingredientId, tagId)
    })

  mRecipes <- sd.recipeNames.keys.toList.traverse(insertRecipe)
//
  ids: Map[Int, Int] = for {
    (recipeName, recipeIngredientNames) <- sd.recipeNames
    recipeIngredientName <- recipeIngredientNames
  } yield {
    val recipeId = getRecipeByName(mRecipes.toList, recipeName).id
    val ingredientId = getIngredientByName(mIngredients, recipeIngredientName).id
    (recipeId, ingredientId)
  }


  mRecipeIngredients <- ids.toList.traverse(x => x match {
    case (recipeId: Int, ingredientId: Int) => insertRecipeIngredient(recipeId, ingredientId)
  })

  mIngredientSets <- sd.ingredientSets.keys.toList.traverse(insertIngredientSet)

  ids = for {
    (setName, setIngredientNames) <- sd.ingredientSets
    ingredientName <- setIngredientNames
  } yield {
    val setId = getSetByName(mIngredientSets.toList, setName).id
    val ingredientId = getIngredientByName(mIngredients, ingredientName).id
    (setId, ingredientId)
  }

  mIngredientSetIngredients <- ids.toList.traverse(x => x match {
    case (setId: Int, ingredientId: Int) => insertIngredientSetIngredient(setId, ingredientId)
  })
} yield None

class DataTools(dbSetup: DBSetup):
  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    connString,
    "postgres", // user
    "" // password
  )

  def getTags(): IO[List[Tag]] = getTagsIO().transact(xa)
  def setup(): IO[Unit] = setupIO(setupDataSimple).transact(xa)
  def getFullRecipes(): IO[List[FullRecipe]] = getFullRecipesIO().transact(xa)
  def getIngredients(): IO[List[FullIngredient]] = getIngredientsIO().transact(xa)
  def getRecipesForIngredients(ingredientUids: List[String]): IO[List[Recipe]] = getRecipesForIngredientsIO(ingredientUids).transact(xa)
  def getIngredientSets(): IO[List[FullIngredientSet]] = getIngredientSetsIO().transact(xa)

object DataTools {


}



