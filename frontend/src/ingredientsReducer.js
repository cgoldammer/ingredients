import { createSlice } from "@reduxjs/toolkit";

export const newValues = (values, additions, removals) => {
  const valsNotRemoved = values.filter(
    (v) => !removals.map((r) => r.uuid).includes(v.uuid)
  );
  return additions.concat(valsNotRemoved);
}

export const ingredientsSelectedSlice = createSlice({
  name: "ingredientsSelected",
  initialState: {
    values: [],
  },
  reducers: {
    setIngredients: (state, actions) => {
      const { additions, removals } = actions.payload;
      state.values = newValues(state.values, additions, removals);
    },
    removeIngredients: (state, actions) => {
      const removals = actions.payload;
      state.values = newValues(state.values, [], removals);
    },
    addIngredients: (state, actions) => {
      const additions = actions.payload;
      state.values = newValues(state.values, additions, []);
    }
  },
});

export const { setIngredients, removeIngredients, addIngredients } = ingredientsSelectedSlice.actions;
export default ingredientsSelectedSlice.reducer;
