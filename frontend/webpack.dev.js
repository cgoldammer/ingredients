const merge = require('webpack-merge').merge;
const webpack = require('webpack');
const common = require('./webpack.common.js');

const devServer = {
  static: './serve_content',
    port: 8082,
    host: '0.0.0.0',
    historyApiFallback: false,
    hot: true,

    headers: {
    "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, PATCH, OPTIONS",
      "Access-Control-Allow-Headers": "X-Requested-With, content-type, Authorization"
  }
}

const modeVal = process.env.NODE_ENV;

const getUrl = modeVal => {
  if (modeVal == "devLocal") return "/fakeApi"
  if (modeVal == "devServer") return "http://127.0.0.1:8200/"
  if (modeVal == "devDocker") return "http://127.0.0.1:8300/"
}

const getFeatures = modeVal => {
  const url = getUrl(modeVal)
  return {BACKENDURL: url}
}

const devExports = modeVal => {
  console.log("mode: " + modeVal);
  console.log("Setting the following variables:")

  const features = getFeatures(modeVal)
  console.log(features);

  return {
    devServer: devServer,
    mode: 'development',
    devtool: "eval-source-map",
    plugins: [
      new webpack.ProvidePlugin({
        process: 'process/browser',
      }),
      new webpack.EnvironmentPlugin(features)
    ]
  }
}

module.exports = merge(common, devExports(modeVal))
