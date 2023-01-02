import { createSlice } from "@reduxjs/toolkit";

export const userSlice = createSlice({
  name: "userData",
  initialState: {
    token: undefined,
  },
  reducers: {
    setToken: (state, actions) => {
      const localStorageKey = "userToken";
      const token = actions.payload;
      state.token = token;
      console.log("SETTING localstorage:" + token);
      if (token != undefined) {
        localStorage.setItem(localStorageKey, token);
      } else {
        localStorage.removeItem(localStorageKey);
      }
    },
  },
});

export const { setToken } = userSlice.actions;
export default userSlice.reducer;
