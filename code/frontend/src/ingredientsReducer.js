import { createSlice } from "@reduxjs/toolkit";

export const newValues = (values, additions, removals) => {
  const valsNotRemoved = values.filter(v => !removals.includes(v));
  return additions.concat(valsNotRemoved);
}

export const ingredientsSelectedSlice = createSlice({
  name: "ingredientsUuidsSelected",
  initialState: {
    values: [],
  },
  reducers: {
    setIngredientsSelected: (state, actions) => {
      const { additions, removals } = actions.payload;
      state.values = newValues(state.values, additions, removals);
    },
    removeIngredientsSelected: (state, actions) => {
      const removals = actions.payload;
      state.values = newValues(state.values, [], removals);
    },
    addIngredientsSelected: (state, actions) => {
      const additions = actions.payload;
      state.values = newValues(state.values, additions, []);
    },
    setIngredientsSelectedSimple: (state, actions) => {
      const values = actions.payload;
      state.values = values;
    }
  },
});

export const { setIngredientsSelected, removeIngredientsSelected, addIngredientsSelected, setIngredientsSelectedSimple } = ingredientsSelectedSlice.actions;
export default ingredientsSelectedSlice.reducer;
