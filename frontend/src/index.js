import React from "react";
import ReactDOM from "react-dom";
import { App } from "./App.jsx";
import { store } from "./store";
import { Provider } from "react-redux";
import { dispatch } from "react-redux";
import { setToken } from "./userReducer";

import { BrowserRouter } from "react-router-dom";
import { apiSlice } from "./features/api/apiSlice";

// if (process.env.RUNMODE != "prod") {
//   import { worker } from "./api/server";
// }

const app = (
  <React.StrictMode>
    <BrowserRouter>
      <Provider store={store}>
        <App />
      </Provider>
    </BrowserRouter>
  </React.StrictMode>
);

if (process.env.RUNMODE != "prod") {
  const apiServer = await import("./api/server");
  const worker = apiServer.worker;
  worker.start({
    onUnhandledRequest: "bypass",
  });
}

const userToken = localStorage.getItem("userToken");
if (userToken != undefined) {
  store.dispatch(setToken(userToken));
}

console.log("Local:" + userToken);
//store.dispatch(apiSlice.endpoints.getUser.initiate());

ReactDOM.render(app, document.getElementById("root"));
