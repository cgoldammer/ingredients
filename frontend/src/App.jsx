import React from "react";

import { IngredientsView } from "./features/ingredients/IngredientsView";

import { ThemeProvider, Box, Typography } from "@mui/material";

import { Route, Routes, useParams } from "react-router-dom";

import { theme } from "./helpers";
import { useGetIngredientsQuery, useGetSampleDataQuery } from "./features/api/apiSlice";
import { DataGrid } from "@mui/x-data-grid";

/* The main app, which pulls in all the other windows. */
export function App() {
  return (
    <ThemeProvider theme={theme}>
      <Box>
        <TopMenu />
        <Box sx={{ my: 2 }}>
          <Routes>
            <Route path="/" element={<IngredientsView />} />
          </Routes>
        </Box>
      </Box>
    </ThemeProvider>
  );
}
