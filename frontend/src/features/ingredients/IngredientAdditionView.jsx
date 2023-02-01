import { useDispatch, useSelector } from "react-redux";
import { useTheme } from "@mui/material/styles";
import { addIngredientsSelected } from "../../ingredientsReducer";
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import Typography from "@mui/material/Typography";
import PropTypes from "prop-types";
import React from "react";
import Box from "@mui/material/Box";

function getStyles(name, personName, theme) {
  return {
    fontWeight:
      personName.indexOf(name) === -1
        ? theme.typography.fontWeightRegular
        : theme.typography.fontWeightMedium,
  };
}
export function IngredientAdditionView(props) {
  const dispatch = useDispatch();
  const theme = useTheme();
  const { tagName, ingredients } = props;
  const ingredientsSelected = useSelector(
    (state) => state.ingredientsSelected.values
  );

  const allIds = ingredients.map((i) => i.uuid);
  const ingredientsDisplayable = ingredientsSelected
    .filter((i) => allIds.includes(i.uuid))
    .map((i) => i.uuid);
  const handleChange = (event) => {
    const {
      target: { value },
    } = event;

    const uuids = value.map((v) => v.uuid);

    dispatch(addIngredientsSelected(uuids));
  };

  return (
    <Box>
      <InputLabel id="demo-multiple-chip-label">{tagName}</InputLabel>
      <Select
        labelId="demo-multiple-chip-label"
        id="demo-multiple-chip"
        multiple
        value={ingredientsDisplayable}
        onChange={handleChange}
        sx={{ minWidth: 300 }}
      >
        {ingredients.map((ingredient) => (
          <MenuItem
            key={ingredient.uuid}
            value={ingredient}
            style={getStyles(name, name, theme)}
          >
            <Typography>
              {ingredient.name}: {ingredient.numberRecipes}
            </Typography>
          </MenuItem>
        ))}
      </Select>
    </Box>
  );
}

IngredientAdditionView.propTypes = {
  ingredients: PropTypes.array,
  tagName: PropTypes.string,
};
