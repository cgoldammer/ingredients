import React from "react";

import { IngredientsView } from "./features/ingredients/IngredientsView";
import { RecipesPossibleView } from "./features/recipes/RecipesPossibleView";
import {LoginView} from "./features/LoginView"

import { ThemeProvider, Box } from "@mui/material";

import { Route, Routes } from "react-router-dom";

import { theme } from "./helpers";

/* The main app, which pulls in all the other windows. */
export function App() {
  return (
    <ThemeProvider theme={theme}>
      <Box>
        <Box sx={{ my: 2 }}>
          <LoginView/>
          {/*<Routes>*/}
          {/*  <Route*/}
          {/*    path="/"*/}
          {/*    element={*/}
          {/*      <div>*/}
          {/*        <IngredientsView />*/}
          {/*        <RecipesPossibleView />*/}
          {/*      </div>*/}
          {/*    }*/}
          {/*  />*/}
          {/*</Routes>*/}
        </Box>
      </Box>
    </ThemeProvider>
  );
}
