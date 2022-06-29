import React from "react";
import PropTypes from "prop-types";
import { useGetIngredientsQuery } from "../api/apiSlice";
import { Box, Typography } from "@mui/material";
import { boxFormat } from "../../helpers";

export function IngredientsView() {
  const { data = { ingredients: [] } } = useGetIngredientsQuery();

  // TODO: A weird hack - I found that `useNavigate` produces
  // react hook error
  // `rendered more hook than during previous render`
  // when used in loops, but I really don't know why
  // So creating it once and passing to the child components
  const ingredientsDisplayed = data.ingredients.map((f) => {
    console.log("Ingredients:" + data.length);
    let f2 = { ...f };
    return f2;
  });

  return (
    <div>{ingredientsDisplayed.map((...args) => IngredientView(...args))}</div>
  );
}

function IngredientView(props) {
  // let navigate = useNavigate();
  const { uuid, name } = props;

  return (
    <Box key={uuid} sx={{ ...boxFormat, height: 180 }}>
      <Box sx={{ display: "grid", gridTemplateRows: "repeat(8, 1fr)" }}>
        <Typography variant="h4">Ingredient: {name}</Typography>
      </Box>
    </Box>
  );
}

IngredientView.propTypes = {
  uuid: PropTypes.String,
  name: PropTypes.String,
  navigate: PropTypes.any,
};