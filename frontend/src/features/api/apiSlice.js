import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { useSelector, useDispatch } from "react-redux";


const url = process.env.BACKENDURL;
console.log("Backend: " + url);


export const apiSlice = createApi({
  reducerPath: "api",
  baseQuery: fetchBaseQuery({ baseUrl: url }),
  tagTypes: ["Ingredient"],
  endpoints: (builder) => ({
    getIngredients: builder.query({
      query: () => "/ingredients",
      providesTags: (result = []) => [
        "Ingredient",
        ...result.ingredients.map(({ id }) => ({ type: "Ingredient", id })),
      ],
    }),
    getRecipesPossible: builder.query({
      query: (user) => ({
        url: "/recipesPossible",
        method: 'POST',
        body: user // Body is automatically converted to json with the correct headers
      }),
      // query: () => "/recipesPossible",
      providesTags: (result = []) => [
        "Recipe",
        ...result.recipes.map(({ id }) => ({ type: "Recipe", id })),
      ],
    })
  }),
});

export const { useGetIngredientsQuery, useGetRecipesPossibleQuery } = apiSlice;
