import React from "react";
import { boxFormat } from "../../helpers";
import { Box, Typography } from "@mui/material";
import PropTypes from "prop-types";

export function RecipeView(props) {
  const { name, uuid } = props;

  return (
    <Box key={uuid} sx={{ ...boxFormat, height: 180 }}>
      <Box sx={{ display: "grid", gridTemplateRows: "repeat(8, 1fr)" }}>
        <Typography variant="h4">{name}</Typography>
      </Box>
    </Box>
  );
}

RecipeView.propTypes = {
  name: PropTypes.String,
  uuid: PropTypes.String,
};
