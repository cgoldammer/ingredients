import { createSlice } from "@reduxjs/toolkit";

export const ingredientSetSelectedSlice = createSlice({
  name: "ingredientSet",
  initialState: {
    value: {},
  },
  reducers: {

    setIngredientSet: (state, actions) => {
        console.log("ACTION");
        console.log(actions.payload);
        state.value = actions.payload
    }
  }
});

export const getSelectedSet = state => {
  const value = state.ingredientsSetsSelected.value
  if ('name' in value){
    return value
  } else {
    return ''
  }
}

export const { setIngredientSet } = ingredientSetSelectedSlice.actions;
export default ingredientSetSelectedSlice.reducer;
