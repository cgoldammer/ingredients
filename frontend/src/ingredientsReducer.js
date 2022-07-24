import { createSlice } from "@reduxjs/toolkit";

export const ingredientsSelectedSlice = createSlice({
  name: "ingredientsSelected",
  initialState: {
    values: [],
  },
  reducers: {
    setIngredients: (state, actions) => {
      const { additions, removals } = actions.payload;
      const valsNotRemoved = state.values.filter(
        (v) => !removals.map((r) => r.uuid).includes(v.uuid)
      );
      state.values = additions.concat(valsNotRemoved);
    },
  },
});

export const { setIngredients } = ingredientsSelectedSlice.actions;
export default ingredientsSelectedSlice.reducer;
