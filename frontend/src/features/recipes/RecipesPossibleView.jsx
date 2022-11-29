import React from "react";
import { useGetRecipesPossibleQuery } from "../api/apiSlice";
import { RecipeView } from "./RecipeView";
import { useSelector } from "react-redux";
import { getIngredientsSelected } from "../../store";
import Grid from "@mui/material/Unstable_Grid2";

export function RecipesPossibleView() {
  const ingredientsSelected = useSelector(getIngredientsSelected);
  const postData = {
    data: ingredientsSelected.map((i) => i.uuid),
    name: "Selected",
  };
  const { data = { data: [] } } = useGetRecipesPossibleQuery(postData);
  const divs = data.data.map((recipe) => (
    <Grid sm={10} md={5} key={recipe.uuid}>
      {RecipeView(recipe)}
    </Grid>
  ));
  return (
    <div>
      <Grid container spacing={2}>
        {divs}
      </Grid>
    </div>
  );
}
