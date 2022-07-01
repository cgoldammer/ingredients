import {createSlice} from "@reduxjs/toolkit";

export const ingredientsSlice = createSlice({
  name: "ingredientsSelected",
  initialState: {
    values: []
  },
  reducers: {
    setIngredients: (state, actions) => {
      const value = actions.payload
      console.log("About to set values")
      console.log(actions)
      state.values = typeof value === "string" ? value.split(",") : value
    }
  }
});

export const {setIngredients} = ingredientsSlice.actions
export default ingredientsSlice.reducer