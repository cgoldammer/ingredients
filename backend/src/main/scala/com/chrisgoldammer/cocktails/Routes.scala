package com.chrisgoldammer.cocktails

import doobie.postgres.*
import doobie.postgres.implicits.*

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

import org.http4s.headers.Origin

//case class RecipeResults(recipes: List[Recipe])
//case class TagResults(tags: List[Tag])
//case class IngredientResults(ingredients: List[FullIngredient])
//case class FullRecipeResult(recipes: List[FullRecipe])


implicit val decT: Decoder[Tag] = deriveDecoder
implicit val encT: Encoder[Tag] = deriveEncoder

implicit val decTR: Decoder[Results[Tag]] = deriveDecoder
implicit val encTR: Encoder[Results[Tag]] = deriveEncoder
implicit val decT2: EntityDecoder[IO, Results[Tag]] = jsonOf[IO, Results[Tag]]


implicit val decI: Decoder[Ingredient] = deriveDecoder
implicit val encI: Encoder[Ingredient] = deriveEncoder

implicit val decIR: Decoder[Results[Ingredient]] = deriveDecoder
implicit val encIR: Encoder[Results[Ingredient]] = deriveEncoder
implicit val decIR2: EntityDecoder[IO, Results[Ingredient]] = jsonOf[IO, Results[Ingredient]]


implicit val decFI: Decoder[FullIngredient] = deriveDecoder
implicit val encFI: Encoder[FullIngredient] = deriveEncoder

implicit val decRR9: Decoder[Results[FullIngredient]] = deriveDecoder
implicit val encRR9: Encoder[Results[FullIngredient]] = deriveEncoder
implicit val decRR99: EntityDecoder[IO, Results[FullIngredient]] = jsonOf[IO, Results[FullIngredient]]


implicit val decMR: Decoder[Recipe] = deriveDecoder
implicit val encMR: Encoder[Recipe] = deriveEncoder

implicit val decRR: Decoder[Results[Recipe]] = deriveDecoder
implicit val encRR: Encoder[Results[Recipe]] = deriveEncoder
implicit val decRR2: EntityDecoder[IO, Results[Recipe]] = jsonOf[IO, Results[Recipe]]


implicit val decI2: Decoder[FullRecipe] = deriveDecoder
implicit val encI2: Encoder[FullRecipe] = deriveEncoder

implicit val decI3: Decoder[Results[FullRecipe]] = deriveDecoder
implicit val encI3: Encoder[Results[FullRecipe]] = deriveEncoder
implicit val decIR23: EntityDecoder[IO, Results[FullRecipe]] = jsonOf[IO, Results[FullRecipe]]


implicit val decISL: Decoder[Results[String]] = deriveDecoder
implicit val encISL: Encoder[Results[String]] = deriveEncoder
implicit val decISL2: EntityDecoder[IO, Results[String]] = jsonOf[IO, Results[String]]


import org.http4s.server.middleware._

val jsonApp = HttpRoutes.of[IO] {
  case GET -> Root / "ingredients" =>
    for {
      ing <- IO {
        Results(getIngredients(), "ingredients").asJson
      }
      resp <- Ok(ing)
    } yield resp
  case GET -> Root / "tags" =>
    for {
      ing <- IO {
        Results(getTags(), "Tags").asJson
      }
      resp <- Ok(ing)
    } yield resp
  case GET -> Root / "recipes" => {
    for {
      ing <- IO {
        Results(getFullRecipes(), "Full Recipes").asJson
      }
      resp <- Ok(ing)
    } yield resp
  }
  case req@POST -> Root / "recipesPossible" => for {
    isl <- req.as[Results[String]]
    j <- IO {
      Results(getRecipesForIngredients(isl.data), "Recipes").asJson
    }
    resp <- Ok(j)
  } yield resp
}.orNotFound

val allowedOrigin = Origin.Host(Uri.Scheme.http, Uri.RegName("localhost"), Some(8082))
val allowedAll = CORS.policy.withAllowOriginAll

val withMiddleWare = CORS.policy
  .withAllowOriginHost(Set(allowedOrigin))
  .withAllowCredentials(false)
  .apply(jsonApp)

val withMiddleWare2 = CORS.policy.withAllowOriginAll(jsonApp)

val server: Resource[IO, org.http4s.server.Server] = EmberServerBuilder
  .default[IO]
  .withHost(ipv4"0.0.0.0")
  .withPort(port"8080")
  .withHttpApp(withMiddleWare2)
  .build