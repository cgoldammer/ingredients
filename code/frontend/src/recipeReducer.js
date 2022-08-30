import { createSlice } from "@reduxjs/toolkit";

export const recipesSlice = createSlice({
  name: "recipes",
  initialState: {
    values: [],
  },
  reducers: {
    setRecipes: (state, actions) => {
      const value = actions.payload;
      state.values = value;
    },
  },
});

export const { setRecipes } = recipesSlice.actions;
export default recipesSlice.reducer;
