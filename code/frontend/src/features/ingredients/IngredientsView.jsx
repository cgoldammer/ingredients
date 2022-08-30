import React from "react";
import { useGetTagsQuery, useGetIngredientsQuery, useGetIngredientSets } from "../api/apiSlice";
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
import { setIngredientsSelected, removeIngredientsSelected, addIngredientsSelected } from "../../ingredientsReducer";
import PropTypes from "prop-types";
import { IngredientSetsView } from "./IngredientSetsView";

import { getIngredientsSelected } from "../../store"

function getStyles(name, personName, theme) {
  return {
    fontWeight:
      personName.indexOf(name) === -1
        ? theme.typography.fontWeightRegular
        : theme.typography.fontWeightMedium,
  };
}

const ITEM_HEIGHT = 48;
const ITEM_PADDING_TOP = 8;
const MenuProps = {
  PaperProps: {
    style: {
      maxHeight: ITEM_HEIGHT * 4.5 + ITEM_PADDING_TOP,
      width: 250,
    },
  },
};

export function IngredientsAdditionView() {
  const dispatch = useDispatch();
  const theme = useTheme();
  const ingredientsSelected = useSelector(getIngredientsSelected)

  const { data = {data: []} } = useGetIngredientsQuery() || {};
  const { data: ingredients = [] } = data || {};

  const ingredientsNotSelected = ingredients.filter(
    (v) => !ingredientsSelected.map((r) => r.uuid).includes(v.uuid)
  );

  const handleChange = event => {
    dispatch(addIngredientsSelected([(event.target.value.uuid)]));
  };

  return (
    <FormControl sx={{ m: 1, minWidth: 200 }}>
      <InputLabel id="demo-simple-select-helper-label">Add Ingredient</InputLabel>
      <Select
        labelId="demo-simple-select-label"
        id="demo-simple-select"
        value=''
        label="Add ingredient"
        labelId="demo-simple-select-helper-label"
        onChange={handleChange}
      >
        {
          ingredientsNotSelected.map(ingredient => (
            <MenuItem key={ingredient.uuid} value={ingredient}>{ingredient.name}</MenuItem>
          ))
        }
      </Select>
    </FormControl>

  )
}

export function IngredientsSelectedView() {
  const dispatch = useDispatch();
  const theme = useTheme();
  const ingredientsSelected = useSelector(getIngredientsSelected)
  const unselectIngredient = (value) => dispatch(removeIngredientsSelected([value.uuid]));

  const buttons = ingredientsSelected.map((ingredient) => (
    <Button
      key={ingredient.uuid}
      onClick={() => unselectIngredient(ingredient)}
    >
      {ingredient.name}
    </Button>
  )
  )

  return (
    <div>{buttons}</div>
  )

}

export function IngredientsWithTagView(props) {
  const dispatch = useDispatch();
  const theme = useTheme();
  const { tagName, ingredients } = props;
  const ingredientsSelected = useSelector(
    (state) => state.ingredientsSelected.values
  );
  const allIds = ingredients.map((i) => i.uuid);
  const ingredientsDisplayable = ingredientsSelected.filter((i) =>
    allIds.includes(i.uuid)
  ).map(i => i.uuid);
  const handleChange = (event) => {
    const {
      target: { value },
    } = event;

    const uuids = value.map(v => v.uuid);

    const additions = uuids.filter(
      (v) => !ingredientsSelected.map((i) => i.uuid).includes(v.uuid)
    );
    const removals = ingredientsDisplayable.filter(
      (v) => !value.map((i) => i.uuid).includes(v.uuid)
    );

    dispatch(
      setIngredientsSelected({ tag: tagName, additions: additions, removals: removals })
    );
  };

  return (
    <div>
      <FormControl sx={{ m: 1, width: 300 }}>
        <InputLabel id="demo-multiple-chip-label">{tagName}</InputLabel>
        <Select
          labelId="demo-multiple-chip-label"
          id="demo-multiple-chip"
          multiple
          value={ingredientsDisplayable}
          onChange={handleChange}
          input={<OutlinedInput id="select-multiple-chip" label="Chip" />}
          renderValue={() => (
            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5 }}>
              {ingredientsDisplayable.map((value) => (
                <Chip key={value.uuid} label={value.name} />
              ))}
            </Box>
          )}
          MenuProps={MenuProps}
        >
          {ingredients.map((ingredient) => (
            <MenuItem
              key={ingredient.uuid}
              value={ingredient}
              style={getStyles(name, name, theme)}
            >
              {ingredient.name}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </div>
  );
}

IngredientsWithTagView.propTypes = {
  ingredients: PropTypes.list,
  tagName: PropTypes.String,
};

const splitByTag = (ingredients, tags) => {
  const findIngredients = (tag) =>
    ingredients.filter((ingredient) =>
      ingredient.tags.map((t) => t.name).includes(tag)
    );
  return tags.reduce(
    (obj, x) => Object.assign(obj, { [x]: findIngredients(x) }),
    {}
  );
};

export function IngredientsView() {
  const { data: ingredientsData } = useGetIngredientsQuery() || {};
  const { ingredients = [] } = ingredientsData || {};
  const { data: tagsData } = useGetTagsQuery() || {};
  const { tags = [] } = tagsData || {};

  var splitIngredients = splitByTag(
    ingredients,
    tags.map((t) => t.name)
  );
  var vals = Object.keys(splitIngredients);
  const views = vals.map((t) => (
    <div key={t}>
      <IngredientsWithTagView tagName={t} ingredients={splitIngredients[t]} />
    </div>
  ));

  return (
    <div>
      <div><span>Sets: </span><IngredientSetsView/></div>
      <div><span>Selected Ingredients: </span><IngredientsSelectedView/></div>
      <div><IngredientsAdditionView/></div>
      {/*<div>{views}</div>*/}
    </div>
  )
}
