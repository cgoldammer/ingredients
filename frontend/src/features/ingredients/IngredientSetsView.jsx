import React, { useState } from "react";
import {
  useGetIngredientSetsQuery,
  useAddIngredientSetMutation,
} from "../api/apiSlice";

import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import ButtonGroup from "@mui/material/ButtonGroup";
import Grid from "@mui/material/Unstable_Grid2";

import { useSelector, useDispatch } from "react-redux";
import { setIngredientSet, getSelectedSet } from "../../ingredientSetsReducer";
import { setIngredientsSelectedSimple } from "../../ingredientsReducer";
import { getIngredientsSelected } from "../../store";
import { listElementsAreIdentical } from "../../helpers/helpers";

export function SaveSetView() {
  const ingredientsSelected = useSelector(getIngredientsSelected);
  const dispatch = useDispatch();
  const { data: ingredientSets } = useGetIngredientSetsQuery();
  const [saveSet] = useAddIngredientSetMutation();
  const [name, setName] = useState("");
  const handleChange = (e) => {
    setName(e.target.value);
  };
  const submit = () => {
    saveSet({
      setName: name,
      ingredientUuids: ingredientsSelected.map((i) => i.uuid),
    }).then(() => {
      dispatch(setIngredientSet(name));
    });
  };
  return (
    <div>
      <TextField
        label="Name of set"
        value={name}
        onChange={handleChange}
      ></TextField>
      <Button onClick={submit}>Save</Button>
    </div>
  );
}

export function IngredientSetsView() {
  const dispatch = useDispatch();
  const skip = useSelector((state) => state.userData.token == undefined);
  const { data: ingredientSetsData } = useGetIngredientSetsQuery(undefined, {
    skip,
  });
  const { data: ingredientSets } = ingredientSetsData || { data: [] };

  const setNames = ingredientSets.map((i) => i.name);
  const selectedSet = useSelector(getSelectedSet);
  const ingredientsSelected = useSelector(getIngredientsSelected);

  const handleChange = (event) => {
    const setName = event.target.value;
    const setChanged = ingredientSets.filter((i) => i.name == setName)[0];
    dispatch(setIngredientSet(setChanged.name));
    dispatch(setIngredientsSelectedSimple(setChanged.ingredients));
  };

  const ingredientsChanged = !listElementsAreIdentical(
    selectedSet != undefined ? selectedSet.ingredients : [],
    ingredientsSelected.map((i) => i.uuid)
  );

  const buttonSelected = (setName) =>
    selectedSet != undefined &&
    selectedSet["name"] == setName &&
    !ingredientsChanged;

  const items = setNames.map((setName) => (
    <Button
      key={setName}
      value={setName}
      onClick={handleChange}
      variant={buttonSelected(setName) ? "contained" : "outlined"}
    >
      {setName}
    </Button>
  ));

  const saveButton = ingredientsChanged ? <SaveSetView /> : <div></div>;

  return (
    <Grid container spacing={1}>
      <Grid md={9}>
        <ButtonGroup label="Select set of ingredients" onChange={handleChange}>
          {items}
        </ButtonGroup>
      </Grid>
      <Grid md={2}>{saveButton}</Grid>
    </Grid>
  );
}
