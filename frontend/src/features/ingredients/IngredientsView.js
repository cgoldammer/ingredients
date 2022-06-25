import { useParams } from "react-router-dom";
import React from "react";
import PropTypes from "prop-types";
import { useNavigate } from "react-router-dom";
import { useGetIngredientsQuery } from "../api/apiSlice";
import { Box, Typography } from "@mui/material";
import { boxFormat, boxImgFormat } from "../../helpers";

export function IngredientsView() {
  let navigate = useNavigate();
  const ingredients = useGetIngredientsQuery();

  // TODO: A weird hack - I found that `useNavigate` produces
  // react hook error
  // `rendered more hook than during previous render`
  // when used in loops, but I really don't know why
  // So creating it once and passing to the child components
  const ingredientsDisplayed = ingredients.map((f) => {
    let f2 = { navigate: navigate, ...f };
    return f2;
  });

  return (
    <div>
      {ingredientsDisplayed.map((...args) => IngredientView(...args))}
    </div>
  );
}

function IngredientView(props) {
  // let navigate = useNavigate();
  const { uuid, name, navigate } = props;
  
  return (
    <Box key={uuid} sx={{ ...boxFormat, height: 180 }}>
      <Box sx={{ display: "grid", gridTemplateRows: "repeat(8, 1fr)" }}>
        <Typography variant="h4">Ingredient: {number}</Typography>
      </Box>
    </Box>
  );
}

IngredientView.propTypes = {
  uuid: PropTypes.String,
  name: PropTypes.String,
  navigate: PropTypes.any,
};
