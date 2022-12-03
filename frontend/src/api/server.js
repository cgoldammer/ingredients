import { rest, setupWorker } from "msw";
import { factory, manyOf, primaryKey } from "@mswjs/data";
import { faker } from "@faker-js/faker";
import seedrandom from "seedrandom";
import { setRandom } from "txtgen";
import { getRange, getRandomSample, getRandomSampleShare } from "../helpers";

// Add an extra delay to all endpoints, so loading spinners show up.
const ARTIFICIAL_DELAY_MS = 1;
const NUM_INGREDIENTS_PER_RECIPE = 2;
const NUM_RECIPES = 5;
const NUM_TAGS = 1;
const NUM_TAGS_PER_INGREDIENT = 1;
const NUM_SETS = 3;

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
    name: String,
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
    description: String,
  },
  ingredientSet: {
    name: primaryKey(String),
    ingredients: Array,
  },
});

const createTagData = () => {
  return {
    name: faker.commerce.productAdjective(),
  };
};

const createIngredientData = (tags) => {
  return {
    uuid: faker.datatype.uuid(),
    name: faker.helpers.unique(faker.commerce.product),
    tags: tags,
  };
};

const createFullRecipeData = (ingredients) => {
  return {
    uuid: faker.datatype.uuid(),
    name: faker.commerce.productName(),
    ingredients: ingredients,
    description: faker.lorem.paragraphs(1),
  };
};

const createIngredientSet = (
  ingredientUuids,
  name = faker.helpers.unique(faker.commerce.product)
) => {
  return {
    name: name,
    ingredients: ingredientUuids,
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

for (let i = 0; i < NUM_SETS; i++) {
  const ingredients = db.ingredient.getAll().map((i) => i.uuid);
  const newSet = createIngredientSet(getRandomSampleShare(ingredients, 0.5));
  db.ingredientSet.create(newSet);
}

export const handlers = [
  rest.get("/fakeApi/ingredients", (req, res, ctx) => {
    const ingredients = {
      data: db.ingredient.getAll().map(serializeIngredient),
    };
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json(ingredients));
  }),
  rest.get("/fakeApi/tags", (req, res, ctx) => {
    const tags = {
      data: db.tag.getAll().map(serializeTag),
    };
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json(tags));
  }),
  rest.get("/fakeApi/ingredient_sets", (req, res, ctx) => {
    const sets = db.ingredientSet.getAll();
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json({ data: sets }));
  }),
  rest.post("/fakeApi/recipes_possible", (req, res, ctx) => {
    const recipes = {
      data: db.fullRecipe.getAll().map(serializeFullRecipe),
    };
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json(recipes));
  }),
  rest.post("/fakeApi/register", (req, res, ctx) => {
    return res(ctx.json("Basic tokenFromServer"));
  }),
  rest.get("/fakeApi/get_user", (req, res, ctx) => {
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json(db.user.getAll()[0]));
  }),
  rest.post("/fakeApi/add_ingredient_set", async (req, res, ctx) => {
    const testResponseData = ["test"];
    const { name, ingredients } = await req.json();
    db.ingredientSet.create(createIngredientSet(ingredients, name));
    return res(ctx.delay(ARTIFICIAL_DELAY_MS), ctx.json(testResponseData));
  }),
];

export const worker = setupWorker(...handlers);
