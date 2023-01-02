import React from "react";
import { boxFormat } from "../../helpers/helpers";
import { Box, Typography, List, ListItemText } from "@mui/material";
import PropTypes from "prop-types";
import Grid from "@mui/material/Unstable_Grid2";

export function RecipeView(props) {
  const { name, uuid, ingredients, description } = props;
  const ingredientsView = ingredients.map((ingredient) => (
    <ListItemText key={ingredient.uuid}>{ingredient.name}</ListItemText>
  ));
  return (
    <Box key={uuid} sx={{ ...boxFormat, height: 200 }}>
      <Grid container spacing={1}>
        <Grid xs={5}>
          <Typography variant="h5">{name}</Typography>
          <List> {ingredientsView}</List>
        </Grid>
        <Grid xs={5}>
          <Typography>{description.substring(0, 110)} </Typography>
        </Grid>
      </Grid>
    </Box>
  );
}

RecipeView.propTypes = {
  name: PropTypes.string,
  uuid: PropTypes.string,
  ingredients: PropTypes.array,
  description: PropTypes.string,
};
