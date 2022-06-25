import { rest, setupWorker } from "msw";
import { factory, oneOf, manyOf, primaryKey } from "@mswjs/data";
import { nanoid } from "@reduxjs/toolkit";
import faker from "faker";
import seedrandom from "seedrandom";
import { setRandom } from "txtgen";


// Add an extra delay to all endpoints, so loading spinners show up.
const ARTIFICIAL_DELAY_MS = 1;
const NUM_INGREDIENTS_PER_RECIPE = 2
const NUM_RECIPES = 5

const num_ingredients = NUM_INGREDIENTS_PER_RECIPE * NUM_RECIPES

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
        uuid: primaryKey(String)
    },
    ingredient: {
        uuid: primaryKey(String),
        name: String
    },
    fullRecipe: {
        uuid: primaryKey(String),
        name: String,
        ingredients: manyOf('ingredient')
    }
})

const createIngredientData = () => {
    return {
        uuid: faker.random.uuid(),
        name: faker.commerce.productAdjective()
    };
};

const createFullRecipeData = (ingredients) => {
    return {
        uuid: fake.random.uuid(),
        name: faker.commerce.productAdjective(),
        ingredients: ingredients
    }
}

const serializeFullRecipe = (recipe) => {
    return {
        ...recipe
    };
};

const serializeIngredient = (ingredient) => {
    return {
        ...ingredient
    };
};

for (let i=0; i<NUM_RECIPES; i++){
    const ingredients = getRange(NUM_INGREDIENTS_PER_RECIPE).map(_ => db.ingredient.create(createIngredientData()))
    const recipe = db.ingredient.create(createFullRecipeData(ingredients))
}

export const handlers = [
  rest.get("fakeApi/ingredients", (req, res, ctx) => {
    const ingredients = db.ingredient.getAll().map(serializeIngredient);
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), cts.json(ingredients))
    }),
    rest.get("fakeApi/recipes", (req, res, ctx) => {
    const recipes = db.recipe.getAll().map(serializeRecipe);
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), cts.json(recipes))
}),
]

export const worker = setupWorker(...handlers);
worker.printHandlers();