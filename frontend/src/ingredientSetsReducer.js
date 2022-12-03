import { createSlice } from "@reduxjs/toolkit";

export const ingredientSetSelectedSlice = createSlice({
  name: "ingredientSet",
  initialState: {
    value: "",
  },
  reducers: {
    setIngredientSet: (state, actions) => {
      state.value = actions.payload;
    },
  },
});

export const getSelectedSet = (state) => {
  const value = state.ingredientsSetsSelected;
  const sets = state.api.queries["getIngredientSets(undefined)"];
  console.log("S SET");
  console.log(sets);
  console.log(value);
  if (value != undefined && sets != undefined && sets.data != undefined) {
    const results = sets.data.data.filter((s) => s.name == value.value);
    if (results.length == 0) {
      return undefined;
    } else {
      return results[0];
    }
  }
};

export const { setIngredientSet } = ingredientSetSelectedSlice.actions;
export default ingredientSetSelectedSlice.reducer;
