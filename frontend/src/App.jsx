import React from "react";

import { IngredientsView } from "./features/ingredients/IngredientsView";
import { RecipesPossibleView } from "./features/recipes/RecipesPossibleView";
import { LoginView, UserView } from "./features/LoginView";
import { TopMenu } from "./features/MenuView";
import Container from "@mui/material/Container";
import { ThemeProvider, Box } from "@mui/material";
import Grid from "@mui/material/Unstable_Grid2";
import CssBaseline from "@mui/material/CssBaseline";

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
      <CssBaseline>
        <Container justifyContent="center" alignItems="center">
          <Grid container justifyContent="center" alignItems="center">
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
          </Grid>
        </Container>
      </CssBaseline>
    </ThemeProvider>
  );
}
