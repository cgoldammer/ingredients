import React from "react";
import { useGetIngredientSetsQuery } from "../api/apiSlice";

import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import Select from "@mui/material/Select";
import { useSelector, useDispatch } from "react-redux";
import { setIngredientSet, getSelectedSet } from "../../ingredientSetsReducer";
import { setIngredientsSelectedSimple } from "../../ingredientsReducer";

export function IngredientSetsView() {
  const dispatch = useDispatch();
  const x = useGetIngredientSetsQuery();
  const { data: ingredientSetsData } = useGetIngredientSetsQuery();
  console.log("X");
  console.log(x);
  console.log(ingredientSetsData);
  const { data: ingredientSets } = ingredientSetsData || { data: [] };
  console.log("Clean");
  console.log(ingredientSets);
  const setNames = ingredientSets.map((i) => i.name);
  const selectedSet = useSelector(getSelectedSet);

  const handleChange = (event) => {
    const setName = event.target.value;
    const setChanged = ingredientSets.filter((i) => i.name == setName)[0];
    dispatch(setIngredientSet(setChanged));
    dispatch(setIngredientsSelectedSimple(setChanged.ingredients));
  };
  const items = setNames.map((setName) => (
    <MenuItem key={setName} value={setName}>
      {setName}
    </MenuItem>
  ));

  return (
    <FormControl sx={{ m: 1, minWidth: 300 }}>
      <InputLabel id="demo-simple-select-helper-label">
        Select set of Ingredients
      </InputLabel>
      <Select
        labelId="demo-simple-select-label"
        id="demo-simple-select"
        value={selectedSet["name"] ?? ""}
        label="Select set of ingredients"
        onChange={handleChange}
      >
        {items}
      </Select>
    </FormControl>
  );
}
