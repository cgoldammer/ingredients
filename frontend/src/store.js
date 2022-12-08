import { configureStore, createListenerMiddleware } from "@reduxjs/toolkit";
import { apiSlice } from "./features/api/apiSlice";
import ingredientsSelectedReducer from "./ingredientsReducer";
import ingredientSetSelectedReducer from "./ingredientSetsReducer";
import userReducer from "./userReducer";
import recipesReducer from "./recipeReducer";
const listenerMiddleware = createListenerMiddleware();
import { createStore, applyMiddleware, compose } from "redux";

// const composeEnhancers =
//   typeof window === "object" && window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__
//     ? window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__({
//         // Specify extension’s options like name, actionsDenylist, actionsCreators, serialize...
//       })
//     : compose;

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

const composeEnhancers =
  (typeof window !== "undefined" &&
    window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__) ||
  compose;

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
