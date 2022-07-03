import React from "react";
import { useGetIngredientsQuery, useGetRecipesPossibleQuery } from "../api/apiSlice";
import { Box } from "@mui/material";

import { useTheme } from "@mui/material/styles";
import OutlinedInput from "@mui/material/OutlinedInput";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import Select from "@mui/material/Select";
import Chip from "@mui/material/Chip";
import { useSelector, useDispatch } from "react-redux";


export function RecipesView() {
  var { data } = useGetIngredientsQuery() || {};
  var { ingredients = [] } = data || {};
  var { data } = useGetRecipesPossibleQuery(ingredients.map(i => i.uuid)) || {};
  var { recipes = [] } = data || {};
  const divs = recipes.map((r) =>
    <div key={r.uuid}> {r.name} </div>
  )
  return (
    <div><div>Recipes Possible</div>
      <div>
        { divs }
      </div>
    </div>
  );
}
