import React from "react";
import ReactDOM from "react-dom";
import { App } from "./App.jsx";
import { store } from "./store";
import { Provider } from "react-redux";

import { BrowserRouter } from "react-router-dom";

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

ReactDOM.render(app, document.getElementById("root"));
