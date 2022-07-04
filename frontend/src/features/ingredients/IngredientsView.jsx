import React from "react";
import { useGetIngredientsQuery } from "../api/apiSlice";
import { Box } from "@mui/material";

import { useTheme } from "@mui/material/styles";
import OutlinedInput from "@mui/material/OutlinedInput";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import Select from "@mui/material/Select";
import Chip from "@mui/material/Chip";
import { useSelector, useDispatch } from "react-redux";
import { setIngredients } from "../../ingredientsReducer";

function getStyles(name, personName, theme) {
  return {
    fontWeight:
      personName.indexOf(name) === -1
        ? theme.typography.fontWeightRegular
        : theme.typography.fontWeightMedium,
  };
}

const ITEM_HEIGHT = 48;
const ITEM_PADDING_TOP = 8;
const MenuProps = {
  PaperProps: {
    style: {
      maxHeight: ITEM_HEIGHT * 4.5 + ITEM_PADDING_TOP,
      width: 250,
    },
  },
};

//
// export const fullSetIngredients => dispatch => values => {
//   const { data } = useGetIngredientsQuery() || {};
//   dispatch(setIngredients(values));
// }


export function IngredientsView() {
  const { data } = useGetIngredientsQuery() || {};
  const { ingredients = [] } = data || {};
  const theme = useTheme();
  const dispatch = useDispatch();
  const ingredientsSelected = useSelector((state) => state.ingredientsSelected);
  const handleChange = (event) => {
    const {
      target: { value },
    } = event;
    console.log(value);
    dispatch(setIngredients(value));
  };

  return (
    <div>
      <FormControl sx={{ m: 1, width: 300 }}>
        <InputLabel id="demo-multiple-chip-label">Ingredients</InputLabel>
        <Select
          labelId="demo-multiple-chip-label"
          id="demo-multiple-chip"
          multiple
          value={ingredientsSelected.values}
          onChange={handleChange}
          input={<OutlinedInput id="select-multiple-chip" label="Chip" />}
          renderValue={() => (
            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5 }}>
              {ingredientsSelected.values.map((value) => (
                <Chip key={value.uuid} label={value.name} />
              ))}
            </Box>
          )}
          MenuProps={MenuProps}
        >
          {ingredients.map((ingredient) => (
            <MenuItem
              key={ingredient.uuid}
              value={ingredient}
              style={getStyles(name, name, theme)}
            >
              {ingredient.name}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </div>
  );
}
