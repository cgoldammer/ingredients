import React, { useState } from "react";
import {
  useGetTagsQuery,
  useGetIngredientsQuery,
  useGetIngredientSetsQuery,
} from "../api/apiSlice";
import { Box } from "@mui/material";

import { useTheme } from "@mui/material/styles";
import OutlinedInput from "@mui/material/OutlinedInput";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import Select from "@mui/material/Select";
import Chip from "@mui/material/Chip";
import Button from "@mui/material/Button";
import { useSelector, useDispatch } from "react-redux";
import {
  removeIngredientsSelected,
  addIngredientsSelected,
  setIngredientsSelectedSimple,
} from "../../ingredientsReducer";
import PropTypes from "prop-types";
import { IngredientSetsView, SaveSetView } from "./IngredientSetsView";
import Grid from "@mui/material/Unstable_Grid2";

import { getIngredientsSelected, userSelector } from "../../store";
import { getSelectedSet, setIngredientSet } from "../../ingredientSetsReducer";
import { listElementsAreIdentical } from "../../helpers/helpers";
import ButtonGroup from "@mui/material/ButtonGroup";
import { RecipeView } from "../recipes/RecipeView";

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
  const ingredientsSelected = useSelector(getIngredientsSelected);

  const { data = { data: [] } } = useGetIngredientsQuery() || {};
  const { data: ingredients } = data || {};

  const ingredientsNotSelected = ingredients.filter(
    (v) => !ingredientsSelected.map((r) => r.uuid).includes(v.uuid)
  );

  const handleChange = (event) => {
    dispatch(addIngredientsSelected([event.target.value.uuid]));
  };

  return (
    <FormControl sx={{ m: 1, minWidth: 200 }}>
      <InputLabel id="demo-simple-select-helper-label">
        Add Ingredient
      </InputLabel>
      <Select
        labelId="demo-simple-select-label"
        id="demo-simple-select"
        value=""
        label="Add ingredient"
        onChange={handleChange}
      >
        {ingredientsNotSelected.map((ingredient) => (
          <MenuItem key={ingredient.uuid} value={ingredient}>
            {ingredient.name}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

export function IngredientsSelectedView() {
  const dispatch = useDispatch();
  const ingredientsSelected = useSelector(getIngredientsSelected);
  const unselectIngredient = (value) =>
    dispatch(removeIngredientsSelected([value.uuid]));

  const buttons = ingredientsSelected.map((ingredient) => (
    <Button
      key={ingredient.uuid}
      onClick={() => unselectIngredient(ingredient)}
    >
      {ingredient.name}
    </Button>
  ));

  return <div>{buttons}</div>;
}

export function IngredientAdditionView(props) {
  const dispatch = useDispatch();
  const theme = useTheme();
  const { tagName, ingredients } = props;
  const ingredientsSelected = useSelector(
    (state) => state.ingredientsSelected.values
  );

  const allIds = ingredients.map((i) => i.uuid);
  const ingredientsDisplayable = ingredientsSelected
    .filter((i) => allIds.includes(i.uuid))
    .map((i) => i.uuid);
  const handleChange = (event) => {
    const {
      target: { value },
    } = event;

    const uuids = value.map((v) => v.uuid);

    dispatch(addIngredientsSelected(uuids));
  };

  return (
    <div>
      <FormControl sx={{ m: 1, width: 150 }}>
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
              {ingredient.name}: {ingredient.numberRecipes}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </div>
  );
}

IngredientAdditionView.propTypes = {
  ingredients: PropTypes.array,
  tagName: PropTypes.string,
};

export function SelectTagsView(props) {
  const { tags, selectedTags, setSelectedTags } = props;

  const clickedOn = (tagName) => {
    var newSelection = [];
    if (isSelected(tagName)) {
      newSelection = selectedTags.filter((i) => i.name != tagName);
    } else {
      const tagAdded = tags.filter((i) => i.name == tagName)[0];
      newSelection = [...newSelection, tagAdded];
    }
    setSelectedTags(newSelection);
  };

  const isSelected = (tagName) =>
    selectedTags.filter((t) => t.name == tagName).length > 0;

  const items = tags
    .map((t) => t.name)
    .map((tagName) => (
      <Button
        key={tagName}
        value={tagName}
        onClick={() => clickedOn(tagName)}
        variant={isSelected(tagName) ? "contained" : "outlined"}
      >
        {tagName}
      </Button>
    ));

  return (
    <Grid container spacing={1}>
      <Grid md={9}>
        <ButtonGroup label="Filter by Tag">{items}</ButtonGroup>
      </Grid>
    </Grid>
  );
}

SelectTagsView.propTypes = {
  tags: PropTypes.array,
  selectedTags: PropTypes.array,
  setSelectedTags: PropTypes.func,
};

export function IngredientsView() {
  const user = useSelector(userSelector);
  const { data: ingredientsData, isSuccess: isSuccessIngredients } =
    useGetIngredientsQuery();
  const ingredients = isSuccessIngredients ? ingredientsData.data : [];

  const ingredientsSelected = useSelector(getIngredientsSelected);

  const { data: tagsData, isSuccess: isSuccessTags } = useGetTagsQuery();
  const tags = isSuccessTags ? tagsData.data : [];
  const [selectedTags, setSelectedTags] = useState([]);

  const ingredientsSelectable = ingredients
    .filter((i) => !ingredientsSelected.map((r) => r.uuid).includes(i.uuid))

    .filter((i) =>
      selectedTags.length == 0
        ? true
        : selectedTags
            .map((t) => t.name)
            .filter((t) => i.tags.map((t) => t.name).includes(t)).length > 0
    );

  const addView = (
    <IngredientAdditionView
      tagName="Pick one"
      ingredients={ingredientsSelectable}
    />
  );

  const setsView =
    user == undefined ? (
      <span />
    ) : (
      <Grid container spacing={1}>
        <Grid xs={2}>Sets:</Grid>
        <Grid xs={9}>
          <IngredientSetsView />
        </Grid>
      </Grid>
    );

  return (
    <div>
      {setsView}
      <Grid container spacing={1}>
        <Grid xs={2}>Total: </Grid>
        <Grid xs={9}>{ingredients.length}</Grid>
        <Grid xs={2}>Selected: </Grid>
        <Grid xs={9}>
          <IngredientsSelectedView />
        </Grid>
        <Grid xs={2}>Add: </Grid>
        <Grid xs={9}>
          <Grid container>
            <SelectTagsView
              tags={tags}
              selectedTags={selectedTags}
              setSelectedTags={setSelectedTags}
            />
          </Grid>
          <Grid container>{addView}</Grid>
        </Grid>
      </Grid>
    </div>
  );
}
