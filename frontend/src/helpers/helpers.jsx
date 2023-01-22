/* A hack for a weird problem: The import is handled differently
when running in webpack-dev-server and through jest. 
Just importing twice, and using the one version that works */

import { createTheme } from "@mui/material";
import { responsiveFontSizes } from "@mui/material/styles";

export const boxFormat = {
  display: "grid",
  boxShadow: 2,
  marginTop: 2,
  marginBottom: 2,
  padding: 2,
  ":hover": {
    cursor: "pointer",
  },
};

export const boxImgFormat = {
  width: 220,
  borderRight: 1,
  borderColor: "grey.500",
};
let theme1 = createTheme({
  typography: {
    fontFamily: ['"Helvetica Neue"'].join(","),
  },
});

export const theme = responsiveFontSizes(theme1);

export const getRange = (max) => Array.from(Array(max), (n, index) => index);

export const getRandomSample = (arr, size) => {
  var shuffled = arr.slice(0),
    i = arr.length,
    temp,
    index;
  while (i--) {
    index = Math.floor((i + 1) * Math.random());
    temp = shuffled[index];
    shuffled[index] = shuffled[i];
    shuffled[i] = temp;
  }

  return shuffled.slice(0, size);
};

export const getRandomSampleShare = (arr, share) =>
  getRandomSample(arr, Math.floor(arr.length * share));

const valuesNotFoundInRight = (a, b) => a.filter((e) => !b.includes(e));

export const listElementsAreIdentical = (a, b) => {
  const valuesNotFound =
    a.length > b.length
      ? valuesNotFoundInRight(a, b)
      : valuesNotFoundInRight(b, a);
  return valuesNotFound.length == 0;
};
