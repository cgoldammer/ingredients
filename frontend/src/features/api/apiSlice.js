import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

const url = process.env.BACKENDURL;
console.log("Backend: " + url)

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
  }),
});

export const {
  useGetIngredientsQuery
} = apiSlice;
