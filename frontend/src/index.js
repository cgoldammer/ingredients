import React from "react";
import ReactDOM from "react-dom";
import { App } from "./App.jsx";
import { store } from "./store";
import { Provider } from "react-redux";
import { worker } from "./api/server";
import { BrowserRouter } from "react-router-dom";

const app = (
  <React.StrictMode>
    <BrowserRouter>
      <Provider store={store}>
        <App />
      </Provider>
    </BrowserRouter>
  </React.StrictMode>
);

worker.start();
store.dispatch(extendedApiSlice.endpoints.getIngredients.initiate());

ReactDOM.render(app, document.getElementById("app"));
module.hot.accept();
