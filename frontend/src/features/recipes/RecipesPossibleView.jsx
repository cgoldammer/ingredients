import React from "react";
import { useGetRecipesPossibleQuery } from "../api/apiSlice";
import { RecipeView } from "./RecipeView";
import { useSelector } from "react-redux";

const baseResult = { recipes: [] };

export function RecipesPossibleView() {
  const ingredientsSelected = useSelector(
    (state) => state.ingredientsSelected.values
  );
  const { data } =
    useGetRecipesPossibleQuery({
      ingredients: ingredientsSelected.map((r) => r.uuid),
    }) || baseResult;
  const { recipes = [] } = data || {};
  const divs = recipes.map(RecipeView);
  return (
    <div>
      <div>{divs}</div>
    </div>
  );
}
