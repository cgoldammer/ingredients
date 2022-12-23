
# Thanks to the cocktail DB for making this data available!
download_letter() {
  curl https://www.thecocktaildb.com/api/json/v1/1/search.php\?f\=$1 > backend/src/main/resources/cocktails/$1.json
}
