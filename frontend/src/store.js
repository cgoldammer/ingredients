import { configureStore, createListenerMiddleware } from "@reduxjs/toolkit";
import { apiSlice } from "./features/api/apiSlice";
import ingredientsSelectedReducer, { setIngredients } from "./ingredientsReducer";
import recipesReducer, { setRecipes } from "./recipeReducer";

const listenerMiddleware = createListenerMiddleware()

listenerMiddleware.startListening({
  actionCreator: setIngredients,
  effect: async (action, listenerApi) => {
    listenerApi.dispatch(apiSlice.endpoints.getRecipesPossible.initiate(action.payload))
  },
})


// listenerMiddleware.startListening({
//   actionCreator: apiSlice.useGetRecipesPossibleQuery,
//   effect: async (action, listenerApi) => {
//     listenerApi.dispatch(apiSlice.endpoints.getRecipesPossible.initiate(action.payload))
//   },
// })

export const store = configureStore({
  reducer: {
    [apiSlice.reducerPath]: apiSlice.reducer,
    ingredientsSelected: ingredientsSelectedReducer,
    recipes: recipesReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().prepend(listenerMiddleware.middleware).concat(apiSlice.middleware),
});
