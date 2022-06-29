/* A hack for a weird problem: The import is handled differently
when running in webpack-dev-server and through jest. 
Just importing twice, and using the one version that works */

import { createTheme } from "@mui/material";

export const boxFormat = {
  display: "grid",
  gridTemplateColumns: "repeat(2, 1fr)",
  boxShadow: 2,
  marginTop: 2,
  marginBottom: 2,
  padding: 2,
  width: 500,
  height: 300,
  ":hover": {
    cursor: "pointer",
  },
};

export const boxImgFormat = {
  width: 220,
  borderRight: 1,
  borderColor: "grey.500",
};
export const theme = createTheme({
  typography: {
    fontFamily: ['"Helvetica Neue"'].join(","),
  },
});

export const getRange = (max) => Array.from(Array(max), (n, index) => index);
