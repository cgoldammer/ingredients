import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

const url = process.env.BACKENDURL;
console.log("Backend: " + url);

export const apiSlice = createApi({
  reducerPath: "api",
  baseQuery: fetchBaseQuery({ baseUrl: url }),
  tagTypes: ["Ingredient", "Recipe"],
  endpoints: (builder) => ({
    getTags: builder.query({
      query: () => "/tags",
      providesTags: (result) => [
        "Tag",
        ...result.data.map(({ id }) => ({ type: "Tag", id })),
      ],
    }),
    getIngredients: builder.query({
      query: () => "/ingredients",
      providesTags: (result) => [
        "Ingredient",
        ...result.data.map(({ id }) => ({ type: "Ingredient", id })),
      ],
    }),
    getIngredientSets: builder.query({
      query: () => "/ingredient_sets"
    }),
    getRecipes: builder.query({
      query: () => "/recipes",
      providesTags: (result) => [
        "Recipe",
        ...result.data.map(({ id }) => ({ type: "Ingredient", id })),
      ],
    }),
    getRecipesPossible: builder.query({
      query: (ingredientSearchList) => ({
        url: "/recipes_possible",
        method: "POST",
        body: ingredientSearchList, // Body is automatically converted to json with the correct headers
      }),
      providesTags: (result) => [
        "Recipe",
        ...result.data.map(({ id }) => ({ type: "Recipe", id })),
      ],
    }),
  }),
});

export const {
  useGetTagsQuery,
  useGetIngredientsQuery,
  useGetIngredientSetsQuery,
  useGetRecipesQuery,
  useGetRecipesPossibleQuery,
} = apiSlice;
