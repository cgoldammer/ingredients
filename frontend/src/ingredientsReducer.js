import { createSlice } from "@reduxjs/toolkit";
import thunkMiddleware from 'redux-thunk'

export const ingredientsSelectedSlice = createSlice({
  name: "ingredientsSelected",
  initialState: {
    values: [],
  },
  reducers: {
    setIngredients: (state, actions) => {
      const value = actions.payload;
      console.log("About to set values");
      console.log(actions);
      state.values = value;
    },
  },
});


export const { setIngredients } = ingredientsSelectedSlice.actions;
export default ingredientsSelectedSlice.reducer;
