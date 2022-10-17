package com.chrisgoldammer.cocktails

import doobie.postgres.*
import doobie.postgres.implicits.*

import cats.effect.*
import org.http4s.*
import org.http4s.circe.jsonOf
import com.chrisgoldammer.cocktails.*
import com.chrisgoldammer.cocktails.data.types.*
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
import cats.data.{Kleisli}


import org.http4s.server.middleware._

case class AuthBackend()
case class AppParams(db: DBSetup, auth: AuthBackend)

val defaultParams = AppParams(DBSetup(), AuthBackend())

type Http4sApp = Kleisli[IO, Request[IO], Response[IO]]

def jsonApp(ap: AppParams): Http4sApp = {

  val dt = DataTools(ap.db)

  HttpRoutes.of[IO] {
    case GET -> Root / "ingredients" =>
      for {
        ing <- for {
          i <- dt.getIngredients()
        } yield Results(i, "ingredients").asJson
        resp <- Ok(ing)
      } yield resp
    case GET -> Root / "tags" =>
      for {
        ing <- for {
          tags <- dt.getTags()
        } yield Results(tags, "Tags").asJson
        resp <- Ok(ing)
      } yield resp
    case GET -> Root / "recipes" => {
      for {
        ing <- for {
          r <- dt.getFullRecipes()
        } yield Results(r, "Full Recipes").asJson
        resp <- Ok(ing)
      } yield resp
    }
    case GET -> Root / "ingredient_sets" => {
      for {
        ing <- for {
          is <- dt.getIngredientSets()
        } yield Results(is, "Ingredient Sets").asJson
        resp <- Ok(ing)
      } yield resp
    }

    case req@POST -> Root / "recipes_possible" => for {
      isl <- req.as[Results[String]]
      j <- for {
        rfi <- dt.getRecipesForIngredients(isl.data)
      } yield Results(rfi, "Recipes").asJson
      resp <- Ok(j)
    } yield resp
    case req@GET -> Root / "login" => logIn.run(req)
    case req@GET -> Root / "authorize_user_from_cookie" => for {
      res <- authorizeUserFromCookie.run(req)
      resp <- Ok(res.toString())
    } yield resp
    case req@GET -> Root / "register" => register.run(req)
    case req@GET -> Root / "example" => Ok("HELLO!")

  }.orNotFound
}



//val allowedOrigin = Origin.Host(Uri.Scheme.http, Uri.RegName("localhost"), Some(8082))
//val allowedAll = CORS.policy.withAllowOriginAll

//val withMiddleWare = CORS.policy
//  .withAllowOriginHost(Set(allowedOrigin))
//  .withAllowCredentials(false)
//  .apply(jsonApp)

val withMiddleWare2 = CORS.policy.withAllowOriginAll(jsonApp(defaultParams))

val server: Resource[IO, org.http4s.server.Server] = EmberServerBuilder
  .default[IO]
  .withHost(ipv4"0.0.0.0")
  .withPort(port"8080")
  .withHttpApp(withMiddleWare2)
  .build