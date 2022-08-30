const ReactRefreshWebpackPlugin = require('@pmmmwh/react-refresh-webpack-plugin');

require.extensions['.css'] = () => {
  return;
};

var path = require('path');
var debug = process.env.NODE_ENV !== "production";
const webpack = require('webpack');

module.exports = {
  entry: [
    './src/index.js'
  ],
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        loader: 'babel-loader',
        options: {
          presets: [ '@babel/preset-env', '@babel/preset-react']
        }
      },
			{
				test: /\.css$/,
				use: [
          'style-loader',
          { 
            loader: 'css-loader',
            options: {modules: true}
          }]
			}
    ]
  },
  resolve: {
    extensions: ['*', '.js', '.jsx']
  },
	devtool: debug ? "eval-source-map" : false,
  output: { path: __dirname, filename: debug ? 'lib/bundle.js' : 'lib/bundle.min.js' },
  plugins: [
    new ReactRefreshWebpackPlugin()
  ],
};
