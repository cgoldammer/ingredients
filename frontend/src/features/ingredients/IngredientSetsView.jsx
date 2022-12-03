import React, { useState } from "react";
import {
  useGetIngredientSetsQuery,
  useAddIngredientSetMutation,
  useRegisterUserMutation,
} from "../api/apiSlice";

import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import ButtonGroup from "@mui/material/ButtonGroup";

import { useSelector, useDispatch } from "react-redux";
import { setIngredientSet, getSelectedSet } from "../../ingredientSetsReducer";
import { setIngredientsSelectedSimple } from "../../ingredientsReducer";
import { getIngredientsSelected } from "../../store";
import { listElementsAreIdentical } from "../../helpers";

export function SaveSetView() {
  const ingredientsSelected = useSelector(getIngredientsSelected);
  const dispatch = useDispatch();
  const { data: ingredientSetsData } = useGetIngredientSetsQuery();
  const { data: ingredientSets } = ingredientSetsData || { data: [] };
  const [saveSet, { data, isSuccess }] = useAddIngredientSetMutation();
  const [name, setName] = useState("");
  const handleChange = (e) => {
    setName(e.target.value);
  };
  const submit = () => {
    saveSet({
      name: name,
      ingredients: ingredientsSelected.map((i) => i.uuid),
    }).then(() => {
      console.log("DATA:" + name);
      console.log(ingredientSets);
      // const set = ingredientSets.filter((i) => i.name == name)[0];
      // console.log("SET:");
      // console.log(set);
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
      <div>Data: {data != undefined ? JSON.stringify(data) : "nothing"} </div>
      <Button onClick={submit}>Save</Button>
    </div>
  );
}

export function IngredientSetsView() {
  const dispatch = useDispatch();
  const { data: ingredientSetsData } = useGetIngredientSetsQuery();
  const { data: ingredientSets } = ingredientSetsData || { data: [] };

  const setNames = ingredientSets.map((i) => i.name);
  const selectedSet = useSelector(getSelectedSet);
  console.log("SELECTED SET");
  console.log(selectedSet);
  const ingredientsSelected = useSelector(getIngredientsSelected);

  const handleChange = (event) => {
    const setName = event.target.value;
    const setChanged = ingredientSets.filter((i) => i.name == setName)[0];
    dispatch(setIngredientSet(setChanged.name));
    dispatch(setIngredientsSelectedSimple(setChanged.ingredients));
  };

  const ingredientsChanged =
    selectedSet != undefined &&
    selectedSet.ingredients != undefined &&
    !listElementsAreIdentical(
      selectedSet.ingredients,
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
    <div>
      <ButtonGroup label="Select set of ingredients" onChange={handleChange}>
        {items}
      </ButtonGroup>

      {saveButton}
    </div>
  );
}
