import React from "react";

import { IngredientsView } from "./features/ingredients/IngredientsView";
import { RecipesPossibleView } from "./features/recipes/RecipesPossibleView";
import { LoginView } from "./features/LoginView";

import { ThemeProvider, Box } from "@mui/material";
import { useGetUserQuery } from "./features/api/apiSlice";

import { Route, Routes } from "react-router-dom";

import { theme } from "./helpers";

/* The main app, which pulls in all the other windows. */
export function App() {
  var { error: userError } = useGetUserQuery();
  userError = userError || { status: 200 };
  const login = userError.status != 200 ? <LoginView /> : <div></div>;
  return (
    <ThemeProvider theme={theme}>
      <Box>
        <Box sx={{ my: 2 }}>
          {login}
          <Routes>
            <Route
              path="/"
              element={
                <div>
                  <IngredientsView />
                  <RecipesPossibleView />
                </div>
              }
            />
          </Routes>
        </Box>
      </Box>
    </ThemeProvider>
  );
}
