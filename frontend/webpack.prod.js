const merge = require('webpack-merge').merge;
const common = require('./webpack.common.js');
const webpack = require('webpack');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;


const prodExports = {
  mode: 'production',
    output: { path: __dirname, filename: 'serve_content/prod/bundle.min.js' },
  devtool: false,
  plugins: [
    new BundleAnalyzerPlugin({openAnalyzer: false, analyzerMode: 'static'})
  ]
}

module.exports = merge(common, prodExports);
