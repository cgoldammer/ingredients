import React, { useState } from "react";
import {
  useGetTagsQuery,
  useGetIngredientsQuery,
  useGetIngredientSetsQuery,
} from "../api/apiSlice";

import Box from "@mui/material/Box";

import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import { useSelector, useDispatch } from "react-redux";
import {
  removeIngredientsSelected,
  addIngredientsSelected,
  setIngredientsSelectedSimple,
} from "../../ingredientsReducer";
import PropTypes from "prop-types";
import { IngredientSetsView } from "./IngredientSetsView";
import Grid from "@mui/material/Unstable_Grid2";

import { getIngredientsSelected, userSelector } from "../../store";
import { IngredientAdditionView } from "./IngredientAdditionView";

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
      <Grid xs={6} key={tagName}>
        <Button
          value={tagName}
          onClick={() => clickedOn(tagName)}
          variant={isSelected(tagName) ? "contained" : "outlined"}
          fullWidth={true}
        >
          <Typography>{tagName}</Typography>
        </Button>
      </Grid>
    ));

  return <Grid container>{items}</Grid>;
}

SelectTagsView.propTypes = {
  tags: PropTypes.array,
  selectedTags: PropTypes.array,
  setSelectedTags: PropTypes.func,
};

const maxTags = 4;

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
      tagName="Add ingredients"
      ingredients={ingredientsSelectable}
    />
  );

  const setsView =
    user == undefined ? (
      <span />
    ) : (
      <Grid container justifyContent="center" alignItems="center" sx={{ m: 2 }}>
        <IngredientSetsView />
      </Grid>
    );

  return (
    <Grid container justifyContent="center" alignItems="center">
      <Grid container sx={{ mt: 2 }}>
        {setsView}
      </Grid>
      <Grid container spacing={1} justifyContent="center" alignItems="center">
        <SelectTagsView
          tags={tags.slice(0, maxTags)}
          selectedTags={selectedTags}
          setSelectedTags={setSelectedTags}
        />
      </Grid>

      <Grid container sx={{ mt: 1 }}>
        {addView}
      </Grid>
      <Grid container>
        <IngredientsSelectedView />
      </Grid>
    </Grid>
  );
}
