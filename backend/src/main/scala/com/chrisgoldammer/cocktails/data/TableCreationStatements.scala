package com.chrisgoldammer.cocktails.data.queries

import java.io.Serial

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

val createTokenDisallowList =
    """
CREATE TABLE token_disallowlist
    (
        token      VARCHAR,
        ts_created timestamp without time zone default (now() at time zone 'utc'),
        PRIMARY KEY (token)
    )
    """
