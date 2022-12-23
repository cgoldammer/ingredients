import React from "react";

import { IngredientsView } from "./features/ingredients/IngredientsView";
import { RecipesPossibleView } from "./features/recipes/RecipesPossibleView";
import { LoginView } from "./features/LoginView";

import { ThemeProvider, Box } from "@mui/material";

import { Route, Routes } from "react-router-dom";

import { theme } from "./helpers";
import { useSelector } from "react-redux";
import { hasUserTokenSelector } from "./store";

/* The main app, which pulls in all the other windows. */
export function App() {
  const hasUser = useSelector(hasUserTokenSelector);
  const login = !hasUser ? <LoginView /> : <div></div>;
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
                  <div>
                    Thanks to{" "}
                    <a href={"https://www.thecocktaildb.com/"}>
                      The Cocktail DB
                    </a>{" "}
                    for their data!
                  </div>
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
