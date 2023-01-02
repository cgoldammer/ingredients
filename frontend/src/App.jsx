import React from "react";

import { IngredientsView } from "./features/ingredients/IngredientsView";
import { RecipesPossibleView } from "./features/recipes/RecipesPossibleView";
import { LoginView, UserView } from "./features/LoginView";
import { TopMenu } from "./features/MenuView";

import { ThemeProvider, Box } from "@mui/material";

import { Route, Routes } from "react-router-dom";

import { theme } from "./helpers/helpers";
import Typography from "@mui/material/Typography";

export function AboutView() {
  return (
    <div>
      <Typography>
        Thanks to <a href={"https://www.thecocktaildb.com/"}>The Cocktail DB</a>{" "}
        for their data!
      </Typography>
    </div>
  );
}

/* The main app, which pulls in all the other windows. */
export function App() {
  return (
    <ThemeProvider theme={theme}>
      <Box>
        <Box sx={{ my: 2 }}>
          <TopMenu />
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
            <Route path="/about" element={<AboutView />} />
            <Route path="/profile" element={<UserView />} />
            <Route path="/register" element={<LoginView />} />
          </Routes>
        </Box>
      </Box>
    </ThemeProvider>
  );
}
