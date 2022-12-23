const merge = require('webpack-merge').merge;
const webpack = require('webpack');
const common = require('./webpack.common.js');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ReactRefreshWebpackPlugin = require("@pmmmwh/react-refresh-webpack-plugin");

const devServer = {
  static: './serve_content',
  port: 8082,
  host: '0.0.0.0',
  historyApiFallback: false,
  hot: true,
  allowedHosts: "all",
  headers: {
  "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, PATCH, OPTIONS",
    "Access-Control-Allow-Headers": "X-Requested-With, content-type, Authorization"
  }
}

const modeVal = process.env.NODE_ENV;

const getUrl = modeVal => {
  if (modeVal == "devMock") return "/fakeApi"
  if (modeVal == "devLocal") return "http://127.0.0.1:8080/"
  if (modeVal == "devServer") return "http://127.0.0.1:8200/"
  if (modeVal == "devDocker") return "http://127.0.0.1:8300/"
  if (modeVal == "prod") return "http://52.22.180.212:8080/"
}

const getFeatures = modeVal => {
  return {
    BACKENDURL: getUrl(modeVal),
    RUNMODE: modeVal}
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
    entry: {
      app: './src/index.js'
    },
    optimization: {
      runtimeChunk: 'single'
    },
    plugins: [
      new webpack.ProvidePlugin({
        process: 'process/browser',
      }),
      new ReactRefreshWebpackPlugin(),
      new webpack.EnvironmentPlugin(features),
      new HtmlWebpackPlugin({
        template: './serve_content/index_old.html',
        filename: 'index.html',
        inject: 'body'
      })
    ]
  }
}

module.exports = merge(common, devExports(modeVal))
