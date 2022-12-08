import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import base64 from "base-64";
const url = process.env.BACKENDURL;
console.log("Backend: " + url);

export const apiSlice = createApi({
  reducerPath: "api",
  baseQuery: fetchBaseQuery({
    baseUrl: url,
    prepareHeaders: (headers, { getState, endpoint }) => {
      const token = getState().userData.token;
      if (token != undefined && endpoint != "registerUser") {
        headers.set("authorization", token);
      }
      return headers;
    },
  }),
  tagTypes: ["User", "Ingredient", "Recipe", "IngredientSet", "Tag"],
  credentials: "include",
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
      query: () => "/ingredient_sets",
      providesTags: ["IngredientSet"],
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
        body: ingredientSearchList,
      }),
      providesTags: (result) => [
        "Recipe",
        ...result.data.map(({ id }) => ({ type: "Recipe", id })),
      ],
    }),
    registerUser: builder.mutation({
      invalidatesTags: ["User", "IngredientSet"],
      query: (data) => {
        const { username, password, isLogin } = data;
        const url = isLogin ? "/login" : "/register";
        return {
          url: url,
          method: "POST",
          headers: {
            authorization: "Basic " + base64.encode(username + ":" + password),
          },
        };
      },
    }),
    getUser: builder.query({
      query: () => "/get_user",
      providesTags: ["User"],
    }),
    addIngredientSet: builder.mutation({
      invalidatesTags: ["IngredientSet"],
      query: (data) => {
        return {
          url: "/add_ingredient_set",
          method: "POST",
          body: data,
        };
      },
    }),
  }),
});

export const {
  useGetTagsQuery,
  useGetIngredientsQuery,
  useGetIngredientSetsQuery,
  useGetRecipesQuery,
  useGetRecipesPossibleQuery,
  useRegisterUserMutation,
  useGetUserQuery,
  useAddIngredientSetMutation,
} = apiSlice;

/* Login process:
- Register -> If successful, returns token in body and logs user in
- Login: If successful, returns token in body  and logs user in

Frontend: Takes token, stores it in localstorage and in redux state. It's send along with
any future requests, and taken from local storage upon app startup.

- Any request: If it contains auth cookie (and cookie hasn't been invalidated),
  backend infers the user from the cookie and provides relevant content.
 */
