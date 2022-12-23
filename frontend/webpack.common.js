

require.extensions['.css'] = () => {
  return;
};

var path = require('path');
var debug = process.env.NODE_ENV !== "production";
const webpack = require('webpack');

const outputDebug = {
  path: path.resolve(__dirname, 'lib'),
    filename: '[name].[fullhash:8].js',
    sourceMapFilename: '[name].[fullhash:8].map',
    chunkFilename: '[id].[fullhash:8].js'
}

const outputNonDebug = { path: __dirname, filename: 'lib/bundle.min.js' }

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
  experiments: {
    topLevelAwait: true
  },
	devtool: debug ? "eval-source-map" : false,
  output: debug ? outputDebug : outputNonDebug,
  plugins: [
  ],
};
