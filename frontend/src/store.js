import { configureStore, createListenerMiddleware } from "@reduxjs/toolkit";
import { apiSlice } from "./features/api/apiSlice";
import ingredientsSelectedReducer from "./ingredientsReducer";
import ingredientSetSelectedReducer from "./ingredientSetsReducer";
import recipesReducer from "./recipeReducer";
const listenerMiddleware = createListenerMiddleware();

export const store = configureStore({
  reducer: {
    [apiSlice.reducerPath]: apiSlice.reducer,
    ingredientsSelected: ingredientsSelectedReducer,
    ingredientsSetsSelected: ingredientSetSelectedReducer,
    recipes: recipesReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware()
      .prepend(listenerMiddleware.middleware)
      .concat(apiSlice.middleware),
});
