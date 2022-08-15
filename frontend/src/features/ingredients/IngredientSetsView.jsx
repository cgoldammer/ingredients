import React from "react";
import { useGetIngredientSetsQuery } from "../api/apiSlice";
import { Box } from "@mui/material";

import { useTheme } from "@mui/material/styles";
import OutlinedInput from "@mui/material/OutlinedInput";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import Select from "@mui/material/Select";
import Chip from "@mui/material/Chip";
import Button from '@mui/material/Button';
import { useSelector, useDispatch } from "react-redux";
import { setIngredientSet, getSelectedSet } from "../../ingredientSetsReducer";
import { setIngredientsSimple } from "../../ingredientsReducer";
import PropTypes from "prop-types";

export function IngredientSetsView() {
  const dispatch = useDispatch();
  const theme = useTheme();
  const ingredientsSelected = useSelector(
    (state) => state.ingredientsSelected.values
  )

  const x = useGetIngredientSetsQuery()
  const { data: ingredientSetsData } = useGetIngredientSetsQuery() || {};
  const ingredientSets = ingredientSetsData || {};
  const setNames = Object.keys(ingredientSets);
  const selectedSet = useSelector(getSelectedSet)

  const handleChange = event => {
    const setName = event.target.value;
    dispatch(setIngredientSet({name: setName, value: ingredientSets[setName]}));
    dispatch(setIngredientsSimple(ingredientSets[setName]));
  };
  const items = (
    setNames.map(setName => (
      <MenuItem key={setName} value={ setName }>{setName}</MenuItem>
    ))
  )

  return (
    <FormControl sx={{ m: 1, minWidth: 300 }}>
      <InputLabel id="demo-simple-select-helper-label">Select set of Ingredients</InputLabel>
      <Select
        labelId="demo-simple-select-label"
        id="demo-simple-select"
        value={selectedSet['name'] ?? ''}
        label="Select set of ingredients"
        labelId="demo-simple-select-helper-label"
        onChange={handleChange}
      >
        { items }
      </Select>
    </FormControl>

  )
}