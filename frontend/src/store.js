import { configureStore, createListenerMiddleware } from "@reduxjs/toolkit";
import { apiSlice } from "./features/api/apiSlice";
import ingredientsSelectedReducer from "./ingredientsReducer";
import ingredientSetSelectedReducer from "./ingredientSetsReducer";
import userReducer from "./userReducer";
import recipesReducer from "./recipeReducer";
const listenerMiddleware = createListenerMiddleware();

export const store = configureStore({
  reducer: {
    [apiSlice.reducerPath]: apiSlice.reducer,
    ingredientsSelected: ingredientsSelectedReducer,
    ingredientsSetsSelected: ingredientSetSelectedReducer,
    recipes: recipesReducer,
    userData: userReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware()
      .prepend(listenerMiddleware.middleware)
      .concat(apiSlice.middleware),
});

export const getIngredientsSelected = (state) => {
  const apiResults = state.api.queries["getIngredients(undefined)"];
  if (apiResults != undefined) {
    if (apiResults["data"] != undefined) {
      const allIngredients = apiResults.data.data;
      const uuidsSelected = state.ingredientsSelected.values;
      const selected = allIngredients.filter((i) =>
        uuidsSelected.includes(i.uuid)
      );
      return selected;
    }
  }
  return [];
};
