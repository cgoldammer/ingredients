package com.chrisgoldammer.cocktails

import cats.effect.*
import org.http4s.*
import org.http4s.circe.jsonOf
import com.chrisgoldammer.cocktails.*
import com.chrisgoldammer.cocktails.data.*

import _root_.io.circe.*
import _root_.io.circe.generic.semiauto.*
import _root_.io.circe.syntax.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.client.dsl.io.*

import org.http4s.ember.server.*
import org.http4s.circe.jsonEncoder
import com.comcast.ip4s.{ipv4, port}


case class RecipeResults(recipes: Array[MRecipeData])
case class IngredientResult(ingredients: Array[Ingredient])

case class User(name: String)
implicit val decI: Decoder[Ingredient] = deriveDecoder
implicit val encI: Encoder[Ingredient] = deriveEncoder

implicit val decMR: Decoder[MRecipeData] = deriveDecoder
implicit val encMR: Encoder[MRecipeData] = deriveEncoder

implicit val decIR: Decoder[IngredientResult] = deriveDecoder
implicit val encIR: Encoder[IngredientResult] = deriveEncoder
implicit val decIR2: EntityDecoder[IO, IngredientResult] = jsonOf[IO, IngredientResult]

implicit val decISL: Decoder[IngredientSearchList] = deriveDecoder
implicit val encISL: Encoder[IngredientSearchList] = deriveEncoder
implicit val decISL2: EntityDecoder[IO, IngredientSearchList] = jsonOf[IO, IngredientSearchList]

implicit val decRR: Decoder[RecipeResults] = deriveDecoder
implicit val encRR: Encoder[RecipeResults] = deriveEncoder
implicit val decRR2: EntityDecoder[IO, RecipeResults] = jsonOf[IO, RecipeResults]


import org.http4s.server.middleware._

val jsonApp = HttpRoutes.of[IO] {
  case GET -> Root / "ingredients"  =>
      for {
        ing <- IO { IngredientResult(getIngredients()).asJson }
        resp <- Ok(ing)
      } yield resp
  case req @ POST -> Root / "recipesThatUseTheseIngredients" => for {
    isl <- req.as[IngredientSearchList]
    j <- IO { RecipeResults(getRecipesForIngredients(isl.ingredients)).asJson }
    resp <- Ok(j)
  } yield resp
}.orNotFound

val server: Resource[IO, org.http4s.server.Server] = EmberServerBuilder
  .default[IO]
  .withHost(ipv4"0.0.0.0")
  .withPort(port"8080")
  .withHttpApp(CORS.policy.withAllowOriginAll(jsonApp))
  .build