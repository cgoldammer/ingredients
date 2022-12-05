import { createSlice } from "@reduxjs/toolkit";

export const userSlice = createSlice({
  name: "userData",
  initialState: {
    token: undefined,
  },
  reducers: {
    setToken: (state, actions) => {
      state.token = actions.payload;
    },
  },
});

export const { setToken } = userSlice.actions;
export default userSlice.reducer;
