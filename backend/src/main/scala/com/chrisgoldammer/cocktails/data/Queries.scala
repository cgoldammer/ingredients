package com.chrisgoldammer.cocktails.data

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.toFoldableOps
import cats.syntax.traverse.*
import doobie.ConnectionIO
import doobie.Fragments
import doobie.HC
import doobie.HPS
import doobie.PreparedStatementIO
import doobie.Update
import doobie.hi.connection
import doobie.implicits.toSqlInterpolator
import doobie.postgres.*
import doobie.postgres.implicits.*

import com.chrisgoldammer.cocktails.cryptocore.*
import com.chrisgoldammer.cocktails.data.types.*

import fs2.Stream

val createUsers =
  """
CREATE TABLE users (
id SERIAL,
name VARCHAR NOT NULL UNIQUE,
uuid VARCHAR NOT NULL UNIQUE,
hash VARCHAR NOT NULL,
is_admin BOOLEAN,
PRIMARY KEY(id)
)
"""

val createUserData =
  """
CREATE TABLE user_data (
id SERIAL,
user_id INT,
PRIMARY KEY(id),
CONSTRAINT fk_userdata_user FOREIGN KEY(user_id) REFERENCES users(id)
)
"""

val createIngredients =
  """
CREATE TABLE ingredients(
id SERIAL,
name VARCHAR NOT NULL UNIQUE,
uuid VARCHAR NOT NULL UNIQUE,
PRIMARY KEY(id)
)
"""

val createRecipe =
  """
CREATE TABLE recipes (
id SERIAL,
name VARCHAR NOT NULL UNIQUE,
uuid VARCHAR NOT NULL UNIQUE,
description VARCHAR,
PRIMARY KEY(id)
)
"""

val createRecipeIngredients =
  """
CREATE TABLE recipe_ingredients (
id SERIAL,
recipe_id INT,
ingredient_id INT,
PRIMARY KEY(id),
CONSTRAINT fk_ingredient FOREIGN KEY(ingredient_id) REFERENCES ingredients(id),
CONSTRAINT fk_recipe FOREIGN KEY(recipe_id) REFERENCES recipes(id)
)
"""

val createTags =
  """
CREATE TABLE tags (
id SERIAL,
name VARCHAR NOT NULL UNIQUE,
PRIMARY KEY(id)
)
"""

val createIngredientTags =
  """
CREATE TABLE ingredient_tags (
id SERIAL,
ingredient_id INT,
tag_id INT,
CONSTRAINT fk_it_i FOREIGN KEY(ingredient_id) REFERENCES ingredients(id),
CONSTRAINT fk_it_t FOREIGN KEY(tag_id) REFERENCES tags(id),
PRIMARY KEY(id)
)
"""

val createIngredientSets =
  """
CREATE TABLE ingredient_sets (
id SERIAL,
name VARCHAR NOT NULL,
uuid VARCHAR NOT NULL UNIQUE,
user_id INT NOT NULL,
PRIMARY KEY(id),
CONSTRAINT fk_ingredientset_user FOREIGN KEY(user_id) REFERENCES users(id)
)
"""

val createIngredientSetsIngredients =
  """
CREATE TABLE ingredient_set_ingredients (
id SERIAL,
ingredient_set_id INT,
ingredient_id INT,
CONSTRAINT fk_is_is FOREIGN KEY(ingredient_set_id) REFERENCES ingredient_sets(id),
CONSTRAINT fk_is_i FOREIGN KEY(ingredient_id) REFERENCES ingredients(id),
 PRIMARY KEY(id)
)
"""

val createStrings: List[String] = List(
  createUsers,
  createUserData,
  createIngredients,
  createRecipe,
  createTags,
  createRecipeIngredients,
  createIngredientTags,
  createIngredientSets,
  createIngredientSetsIngredients
)

def dropString(tableName: String): String =
  f"DROP TABLE IF EXISTS $tableName%s CASCADE"
def tableNames = List(
  "ingredient_set_ingredients",
  "ingredient_sets",
  "ingredient_tags",
  "tags",
  "recipe_ingredients",
  "recipes",
  "ingredients",
  "user_data",
  "users"
)
// def stringToSqlBasic(sqlString: String) = ???
def updater(sqlString: String): Stream[ConnectionIO, Unit] =
  HC.updateWithGeneratedKeys(List())(sqlString, HPS.set(()), 512)
def stringToSqlBasic(sqlString: String) = updater(sqlString).compile.drain
val dropTables: ConnectionIO[Unit] = tableNames.traverse_ { table =>
  stringToSqlBasic(dropString(table))
}

val createTables: ConnectionIO[Unit] = createStrings.traverse_ { sqlString =>
  stringToSqlBasic(sqlString)
}

val recipeIngredientDataQuery =
  sql"""
       |SELECT r.name, r.uuid, i.name AS ingredient_name, i.uuid AS ingredient_uuid
FROM recipe_ingredients ri
JOIN recipes r ON ri.recipe_id=r.id
JOIN ingredients i ON ri.ingredient_id=i.id
""".stripMargin

def insertIngredientSet(
    name: String,
    userId: Int
): ConnectionIO[StoredElement[IngredientSet]] = {
  val uuid = getUuid()
  sql"INSERT INTO ingredient_sets (name, uuid, user_id) values ($name, $uuid, $userId)".update
    .withUniqueGeneratedKeys("id", "name", "uuid")
}

def insertIngredientSetIngredient(
    setId: Int,
    ingredientId: Int
): ConnectionIO[StoredElement[IngredientSetIngredient]] = {
  sql"INSERT INTO ingredient_set_ingredients (ingredient_set_id, ingredient_id) values ($setId, $ingredientId)".update
    .withUniqueGeneratedKeys("id", "ingredient_set_id", "ingredient_id")
}

def bulkInsertIngredientSetIngredientWithValues(
    setId: Int,
    ingredientUuids: NonEmptyList[String]
): ConnectionIO[Int] = {
  val inFragment = Fragments.in(fr"i.uuid", ingredientUuids)

  val insertSql =
    sql"""
    INSERT INTO ingredient_set_ingredients (ingredient_set_id, ingredient_id)
    SELECT $setId AS ingredient_set_id, id AS ingredient_id
    FROM ingredients i WHERE """ ++ inFragment
  insertSql.update.run
}

def bulkCreateIngredientSet(
    setName: String,
    userUuid: String,
    ingredientUuids: List[String]
): ConnectionIO[Int] = for {
  userIdOption <- getUserIdByUuid(userUuid)
  ids <- userIdOption match {
    case Some(userId) =>
      for {
        insertedSet <- insertIngredientSet(setName, userId)
        ids <- bulkInsertIngredientSetIngredient(
          insertedSet.id,
          ingredientUuids
        )
//      ids = List()
      } yield ids
    case None => 0.pure[ConnectionIO]
  }
} yield ids

def bulkInsertIngredientSetIngredient(
    setId: Int,
    ingredientUuids: List[String]
): ConnectionIO[Int] = {
  NonEmptyList
    .fromList(ingredientUuids)
    .map(sn => bulkInsertIngredientSetIngredientWithValues(setId, sn))
    .getOrElse(0.pure[ConnectionIO])
}

def insertIngredientTag(
    ingredientId: Int,
    tagId: Int
): ConnectionIO[StoredElement[IngredientTag]] = {
  sql"INSERT INTO ingredient_tags (ingredient_id, tag_id) values ($ingredientId, $tagId)".update
    .withUniqueGeneratedKeys("id", "ingredient_id", "tag_id")
}

def insertTag(name: String): ConnectionIO[StoredElement[Tag]] = {
  sql"INSERT INTO tags (name) values ($name)".update
    .withUniqueGeneratedKeys("id", "name")
}

def insertIngredient(name: String): ConnectionIO[StoredElement[Ingredient]] = {
  val uuid = getUuid()
  sql"INSERT INTO ingredients (name, uuid) values ($name, $uuid)".update
    .withUniqueGeneratedKeys("id", "name", "uuid")
}

def insertRecipeIngredient(
    recipeId: Int,
    ingredientId: Int
): ConnectionIO[StoredElement[RecipeIngredient]] = {
  sql"INSERT INTO recipe_ingredients (recipe_id, ingredient_id) values ($recipeId, $ingredientId)".update
    .withUniqueGeneratedKeys("id", "recipe_id", "ingredient_id")
}

def insertRecipe(
    name: String,
    description: String
): ConnectionIO[StoredElement[Recipe]] = {
  val uuid = getUuid()
  sql"INSERT INTO recipes (name, uuid, description) values ($name, $uuid, $description)".update
    .withUniqueGeneratedKeys("id", "name", "uuid", "description")
}

def getUserIdByUuid(uuid: String): ConnectionIO[Option[Int]] =
  sql"""SELECT id FROM users WHERE uuid=$uuid"""
    .query[Int]
    .to[List]
    .map(_.headOption)

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
    SELECT
      r.name, r.uuid, r.description
    , i.name, i.uuid
    FROM recipes r
    JOIN recipeNumberIngredients rni
    ON r.id=rni.recipe_id
    JOIN recipeNumberFound rnf
    ON r.id=rnf.recipe_id
    JOIN recipe_ingredients ri
    ON r.id=ri.recipe_id
    JOIN ingredients i
    ON ri.ingredient_id=i.id
    WHERE num_ing_found=num_ing_total
    """
}

val getIngredientsQuery = sql"""
  with counts AS (
    SELECT ingredient_id, COUNT(distinct recipe_id) AS number_recipes
    FROM recipe_ingredients
    GROUP BY ingredient_id
), base AS (
    SELECT i.id, min(i.name) as name, min(uuid) as uuid, array_remove(array_agg(t.name), NULL) as tags
    FROM ingredients i
     LEFT JOIN ingredient_tags it on i.id = it.ingredient_id
     LEFT JOIN tags t ON it.tag_id = t.id
    GROUP BY i.id)
  SELECT
    b.*, number_recipes FROM base b
    JOIN counts c
    ON b.id=c.ingredient_id
    ORDER BY number_recipes DESC
  """

def getIngredientsSetsStoredQuery(userUuid: String) = sql"""
with base AS (
  SELECT s.id, s.name, s.uuid, i.uuid AS ingredient_uuid
  FROM ingredient_sets s
  JOIN users u ON s.user_id = u.id
  JOIN ingredient_set_ingredients isi ON s.id=isi.ingredient_set_id
  JOIN ingredients i ON isi.ingredient_id=i.id
  WHERE u.uuid = $userUuid
  )
SELECT id, min(name) as name, min(uuid) as uuid, array_agg(ingredient_uuid) as ingredient_uuids
FROM base group by id
"""

def getCount(tableName: String): ConnectionIO[Int] = {
  val sqlString = f"SELECT count(*) FROM $tableName"
  val sql = HC.stream[(Int)](sqlString, ().pure[PreparedStatementIO], 512)
  sql.compile.toList.map(_.head)
}
