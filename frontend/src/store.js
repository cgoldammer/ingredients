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

export const getIngredientsSelected = state => {
  const apiResults = state.api.queries['getIngredients(undefined)']
  if (apiResults != undefined) {
    if (apiResults['data'] != undefined){
      const allIngredients = apiResults.data.ingredients

      const uuidsSelected = state.ingredientsSelected.values

      console.log("Ingredients: " + allIngredients.length + " | Selected: " + uuidsSelected.length)
      const selected = allIngredients.filter(i => uuidsSelected.includes(i.uuid))

      console.log("Results selected")
      console.log(selected)
      return(selected)
    }

  }
  return []

}
