import React from "react";
import {useGetRecipesPossibleQuery} from "../api/apiSlice";
import {RecipeView} from './RecipeView'
import {useSelector} from "react-redux";

import {ThemeProvider, Box, Typography, H1} from "@mui/material";

const baseResult = {'recipes': []}

export function RecipesPossibleView() {
  const ingredientsSelected = useSelector((state) => state.ingredientsSelected.values);
  var {data} = useGetRecipesPossibleQuery({'ingredients': ingredientsSelected.map(r => r.uuid)}) || baseResult;
  var {recipes = []} = data || {};
  // var recipes = []
  const divs = recipes.map(RecipeView)
  return (
    <div>
      <div>
        {divs}
      </div>
    </div>
  );
}
