import { createSlice } from "@reduxjs/toolkit";

export const ingredientsSelectedSlice = createSlice({
  name: "ingredientsSelected",
  initialState: {
    values: [],
  },
  reducers: {
    setIngredients: (state, actions) => {
      const { additions, removals } = actions.payload;
      console.log("About to set values");
      console.log(actions);
      console.log("REmomvals");
      console.log(removals);
      const valsNotRemoved = state.values.filter(
        (v) => !removals.map((r) => r.uuid).includes(v.uuid)
      );
      state.values = additions.concat(valsNotRemoved);
    },
  },
});

export const { setIngredients } = ingredientsSelectedSlice.actions;
export default ingredientsSelectedSlice.reducer;
