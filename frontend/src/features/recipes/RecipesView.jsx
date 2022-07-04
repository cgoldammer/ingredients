import React from "react";
import {useGetRecipesQuery} from "../api/apiSlice";
import {useSelector} from "react-redux";
import {RecipeView} from './RecipeView'

const baseResult = {'recipes': []}


export function RecipesView() {
  var {data} = useGetRecipesQuery() || baseResult;
  var {recipes = []} = data || {};
  const divs = recipes.map(RecipeView);
  return (
    <div>
      <div>All Recipes</div>
      <div>
        {divs}
      </div>
    </div>
  );
}
