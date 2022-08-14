import { rest, setupWorker } from "msw";
import { factory, manyOf, primaryKey } from "@mswjs/data";
import faker from "faker";
import seedrandom from "seedrandom";
import { setRandom } from "txtgen";
import { getRange, getRandomSample } from "../helpers";

// Add an extra delay to all endpoints, so loading spinners show up.
const ARTIFICIAL_DELAY_MS = 1;
const NUM_INGREDIENTS_PER_RECIPE = 2;
const NUM_RECIPES = 5;
const NUM_TAGS = 5;
const NUM_TAGS_PER_INGREDIENT = 2;

/* RNG setup */
// Set up a seeded random number generator, so that we get
// a consistent set of users / entries each time the page loads.
// This can be reset by deleting this localStorage value,
// or turned off by setting `useSeededRNG` to false.
let useSeededRNG = true;

let rng = seedrandom();

if (useSeededRNG) {
  let randomSeedString = localStorage.getItem("randomTimestampSeed");
  let seedDate;

  if (randomSeedString) {
    seedDate = new Date(randomSeedString);
  } else {
    seedDate = new Date();
    randomSeedString = seedDate.toISOString();
    localStorage.setItem("randomTimestampSeed", randomSeedString);
  }

  rng = seedrandom(randomSeedString);
  setRandom(rng);
  faker.seed(seedDate.getTime());
}

export const db = factory({
  user: {
    uuid: primaryKey(String),
  },
  tag: {
    name: primaryKey(String),
  },
  ingredient: {
    uuid: primaryKey(String),
    name: String,
    tags: manyOf("tag"),
  },
  fullRecipe: {
    uuid: primaryKey(String),
    name: String,
    ingredients: manyOf("ingredient"),
  },
});

const createTagData = () => {
  return {
    name: faker.commerce.productAdjective(),
  };
};

const createIngredientData = (tags) => {
  return {
    uuid: faker.random.uuid(),
    name: faker.commerce.product(),
    tags: tags,
  };
};

const createFullRecipeData = (ingredients) => {
  return {
    uuid: faker.random.uuid(),
    name: faker.commerce.productName(),
    ingredients: ingredients,
  };
};

const serializeFullRecipe = (recipe) => {
  return {
    ...recipe,
  };
};

const serializeIngredient = (ingredient) => {
  return {
    ...ingredient,
  };
};

const serializeTag = (tag) => {
  return {
    ...tag,
  };
};

const tags = getRange(NUM_TAGS).map(() => db.tag.create(createTagData()));

for (let i = 0; i < NUM_RECIPES; i++) {
  const ingredients = getRange(NUM_INGREDIENTS_PER_RECIPE).map(() => {
    const tagsForIngredient = getRandomSample(tags, NUM_TAGS_PER_INGREDIENT);
    return db.ingredient.create(createIngredientData(tagsForIngredient));
  });
  db.fullRecipe.create(createFullRecipeData(ingredients));
}

export const handlers = [
  rest.get("/fakeApi/ingredients", (req, res, ctx) => {
    const ingredients = {
      ingredients: db.ingredient.getAll().map(serializeIngredient),
    };
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json(ingredients));
  }),
  rest.get("/fakeApi/tags", (req, res, ctx) => {
    const tags = {
      tags: db.tag.getAll().map(serializeTag),
    };
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json(tags));
  }),
  rest.get("/fakeApi/ingredientSets", (req, res, ctx) => {
    const ingredients = {
      ingredients: db.ingredient.getAll().map(serializeIngredient),
    };
    const firstHalf = getRange(parseInt(ingredients.length / 2))
    const firstHalfSet = firstHalf.map(i => ingredients[i])
    const ingredientSets = {'firstHalf': firstHalfSet}
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json(ingredientSets));
  }),
  rest.post("/fakeApi/recipesPossible", (req, res, ctx) => {
    const recipes = {
      recipes: db.fullRecipe.getAll().map(serializeFullRecipe),
    };
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json(recipes));
  }),
];

export const worker = setupWorker(...handlers);
worker.printHandlers();
