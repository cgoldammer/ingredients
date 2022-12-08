import { configureStore, createListenerMiddleware } from "@reduxjs/toolkit";
import { apiSlice } from "./features/api/apiSlice";
import ingredientsSelectedReducer from "./ingredientsReducer";
import ingredientSetSelectedReducer from "./ingredientSetsReducer";
import userReducer from "./userReducer";
import recipesReducer from "./recipeReducer";
const listenerMiddleware = createListenerMiddleware();

const reducer = {
  [apiSlice.reducerPath]: apiSlice.reducer,
  ingredientsSelected: ingredientsSelectedReducer,
  ingredientsSetsSelected: ingredientSetSelectedReducer,
  recipes: recipesReducer,
  userData: userReducer,
};

const middleware = (getDefaultMiddleware) =>
  getDefaultMiddleware()
    .prepend(listenerMiddleware.middleware)
    .concat(apiSlice.middleware);

export const store = configureStore({
  reducer: reducer,
  middleware: middleware,
});

export const getIngredientsSelected = (state) => {
  const apiResults = state.api.queries["getIngredients(undefined)"];
  if (apiResults != undefined) {
    if (apiResults.data != undefined) {
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

export const getUserTokenSelector = (state) => state.userData.token;
export const hasUserTokenSelector = (state) =>
  getUserTokenSelector(state) != undefined;
