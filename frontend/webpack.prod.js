const merge = require('webpack-merge').merge;
const common = require('./webpack.common.js');
const webpack = require('webpack');
const { IgnorePlugin } = require('webpack');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const features = {BACKENDURL: '/api/', RUNMODE: 'prod'}

const prodExports = {
  mode: 'production',
  output: { path: __dirname, filename: 'serve_content/prod/bundle.min.js' },
  devtool: false,

  plugins: [
    new BundleAnalyzerPlugin({openAnalyzer: false, analyzerMode: 'static'}),
    new webpack.EnvironmentPlugin(features),
    new IgnorePlugin({resourceRegExp: /@faker-js/}),
  ]
}

module.exports = merge(common, prodExports);
