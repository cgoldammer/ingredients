import React from "react";
import { useGetIngredientSetsQuery } from "../api/apiSlice";

import Button from "@mui/material/Button";
import ButtonGroup from "@mui/material/ButtonGroup";

import { useSelector, useDispatch } from "react-redux";
import { setIngredientSet, getSelectedSet } from "../../ingredientSetsReducer";
import { setIngredientsSelectedSimple } from "../../ingredientsReducer";

export function IngredientSetsView() {
  const dispatch = useDispatch();
  const { data: ingredientSetsData } = useGetIngredientSetsQuery();

  const { data: ingredientSets } = ingredientSetsData || { data: [] };
  const setNames = ingredientSets.map((i) => i.name);
  const selectedSet = useSelector(getSelectedSet);

  const handleChange = (event) => {
    const setName = event.target.value;
    const setChanged = ingredientSets.filter((i) => i.name == setName)[0];
    dispatch(setIngredientSet(setChanged));
    dispatch(setIngredientsSelectedSimple(setChanged.ingredients));
  };
  const items = setNames.map((setName) => (
    <Button
      key={setName}
      value={setName}
      onClick={handleChange}
      variant={selectedSet["name"] == setName ? "contained" : "outlined"}
    >
      {setName}
    </Button>
  ));

  return (
    <ButtonGroup label="Select set of ingredients" onChange={handleChange}>
      {items}
    </ButtonGroup>
  );
}
