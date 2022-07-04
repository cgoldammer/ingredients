import React from "react";

export function RecipeView(props) {
  const {name, uuid} = props

  return (
    <div key={uuid}>{name}</div>
  )

}