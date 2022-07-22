import React from "react";
import {boxFormat, boxImgFormat} from "../../helpers";
import {ThemeProvider, Box, Typography, H1} from "@mui/material";

export function RecipeView(props) {
  const {name, uuid} = props

  return (
    <Box key={uuid} sx={{...boxFormat, height: 180}}>
      <Box sx={{display: "grid", gridTemplateRows: "repeat(8, 1fr)"}}>
        <Typography variant="h4">{name}</Typography>
      </Box>
    </Box>
  )

}