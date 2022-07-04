import {createApi, fetchBaseQuery} from "@reduxjs/toolkit/query/react";
import {useSelector, useDispatch} from "react-redux";


const url = process.env.BACKENDURL;
console.log("Backend: " + url);


export const apiSlice = createApi({
  reducerPath: "api",
  baseQuery: fetchBaseQuery({baseUrl: url}),
  tagTypes: ["Ingredient", "Recipe"],
  endpoints: (builder) => ({
    getIngredients: builder.query({
      query: () => "/ingredients",
      providesTags: (result = []) => [
        "Ingredient",
        ...result.ingredients.map(({id}) => ({type: "Ingredient", id})),
      ],
    }),
    getRecipes: builder.query({
      query: () => "/recipes",
      providesTags: (result = []) => [
        "Recipe",
        ...result.recipes.map(({id}) => ({type: "Ingredient", id})),
      ],
    }),
    getRecipesPossible: builder.query({
      query: (ingredientSearchList) => ({
        url: "/recipesPossible",
        method: 'POST',
        body: ingredientSearchList // Body is automatically converted to json with the correct headers
      }),
      providesTags: (result = []) => [
        "Recipe",
        ...result.recipes.map(({id}) => ({type: "Recipe", id})),
      ],
    })
  }),
});

export const {useGetIngredientsQuery, useGetRecipesQuery, useGetRecipesPossibleQuery} = apiSlice;
