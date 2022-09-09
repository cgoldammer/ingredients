import React from "react";
import { useGetRecipesPossibleQuery } from "../api/apiSlice";
import { RecipeView } from "./RecipeView";
import { useSelector } from "react-redux";
import { getIngredientsSelected } from "../../store";

export function RecipesPossibleView() {
  const ingredientsSelected = useSelector(getIngredientsSelected);
  const postData = {
    data: ingredientsSelected.map((i) => i.uuid),
    name: "Selected",
  };
  console.log("POST DATA");
  console.log(postData);

  const { data = { data: [] } } = useGetRecipesPossibleQuery(postData);
  console.log("DATA IS");
  console.log(data);
  const divs = data.data.map(RecipeView);
  return (
    <div>
      <div>{divs}</div>
    </div>
  );
}
