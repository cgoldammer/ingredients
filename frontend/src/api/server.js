import { rest, setupWorker } from "msw";
import { factory, manyOf, primaryKey } from "@mswjs/data";
import faker from "faker";
import seedrandom from "seedrandom";
import { setRandom } from "txtgen";
import { getRange } from "../helpers";

// Add an extra delay to all endpoints, so loading spinners show up.
const ARTIFICIAL_DELAY_MS = 1;
const NUM_INGREDIENTS_PER_RECIPE = 2;
const NUM_RECIPES = 5;

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
  ingredient: {
    uuid: primaryKey(String),
    name: String,
  },
  fullRecipe: {
    uuid: primaryKey(String),
    name: String,
    ingredients: manyOf("ingredient"),
  },
});

const createIngredientData = () => {
  return {
    uuid: faker.random.uuid(),
    name: faker.commerce.productAdjective(),
  };
};

const createFullRecipeData = (ingredients) => {
  return {
    uuid: faker.random.uuid(),
    name: faker.commerce.productAdjective(),
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

for (let i = 0; i < NUM_RECIPES; i++) {
  const ingredients = getRange(NUM_INGREDIENTS_PER_RECIPE).map(() =>
    db.ingredient.create(createIngredientData())
  );
  db.fullRecipe.create(createFullRecipeData(ingredients));
}

export const handlers = [
  rest.get("/fakeApi/ingredients", (req, res, ctx) => {
    const ingredients = {
      ingredients: db.ingredient.getAll().map(serializeIngredient),
    };
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json(ingredients));
  }),
  rest.get("/fakeApi/recipes", (req, res, ctx) => {
    const recipes = db.recipe.getAll().map(serializeFullRecipe);
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json(recipes));
  }),
];

export const worker = setupWorker(...handlers);
worker.printHandlers();
