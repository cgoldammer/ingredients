import { createSlice } from "@reduxjs/toolkit";

export const recipesSlice = createSlice({
  name: "recipes",
  initialState: {
    values: [],
  },
  reducers: {
    setRecipes: (state, actions) => {
      const value = actions.payload;
      console.log("About to set recipes");
      console.log(actions);
      state.values = value;
    },
  },
  // extraReducers: (builder) => {
  //   builder.addMatcher(
  //     (action) => {
  //       console.log(action)
  //       return action.type.endsWith('/fulfilled') && (action.payload.ingredients != undefined)
  //     },
  //     (state, { payload } ) => {
  //       console.log("Received payload" + payload)
  //
  //     }
  //   )
  // }
});

export const { setRecipes } = recipesSlice.actions;
export default recipesSlice.reducer;
