import { configureStore, createListenerMiddleware } from "@reduxjs/toolkit";
import { apiSlice } from "./features/api/apiSlice";
import ingredientsSelectedReducer from "./ingredientsReducer";
import recipesReducer from "./recipeReducer";
const listenerMiddleware = createListenerMiddleware();

export const store = configureStore({
  reducer: {
    [apiSlice.reducerPath]: apiSlice.reducer,
    ingredientsSelected: ingredientsSelectedReducer,
    recipes: recipesReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware()
      .prepend(listenerMiddleware.middleware)
      .concat(apiSlice.middleware),
});
