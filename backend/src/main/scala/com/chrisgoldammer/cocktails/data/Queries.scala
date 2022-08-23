package com.chrisgoldammer.cocktails.queries

import doobie.*
import doobie.implicits.*
import cats.implicits.*
import fs2.Stream
import cats.syntax.traverse.*
import doobie.hi.connection
import doobie.postgres.*
import doobie.postgres.implicits.*

//import scala.collection.immutable.Stream

val q =
  """
  select code, name, population, gnp
  from country
  where population > ?
  and   population < ?
  """

val sqlC: Stream[ConnectionIO, String] = HC.stream[String](q, ().pure[PreparedStatementIO], 512)

val sqlA = sql"select 43"
val sqlB = sql"select random()"


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
name VARCHAR NOT NULL UNIQUE,
PRIMARY KEY(id)
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
  createIngredients,
  createRecipe,
  createTags,
  createRecipeIngredients,
  createIngredientTags,
  createIngredientSets,
  createIngredientSetsIngredients
)

def dropString(tableName: String): String = f"DROP TABLE IF EXISTS $tableName%s"
def tableNames = List("ingredient_set_ingredients", "ingredient_sets",
  "ingredient_tags", "tags", "recipe_ingredients", "recipes", "ingredients")
def stringToSqlBasic(sqlString: String) = HC.updateWithGeneratedKeys(List())(sqlString, HPS.set(()), 512).compile.drain
val dropTables: ConnectionIO[Unit] = tableNames.traverse_ {
  table => stringToSqlBasic(dropString(table))
}

val createTables: ConnectionIO[Unit] = createStrings.traverse_ {
  sqlString => stringToSqlBasic(sqlString)
}

