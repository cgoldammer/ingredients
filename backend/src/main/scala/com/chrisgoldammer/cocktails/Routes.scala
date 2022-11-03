package com.chrisgoldammer.cocktails

import doobie.postgres.*
import doobie.postgres.implicits.*

import cats._, cats.effect._, cats.implicits._, cats.data._
import org.http4s.*
import org.http4s.server._
import org.http4s.circe.jsonOf
import com.chrisgoldammer.cocktails.*
import com.chrisgoldammer.cocktails.cryptocore.*
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

case class AppParams(db: DBSetup, auth: AuthBackend = AuthBackend.Doobie)

type Http4sApp = Kleisli[IO, Request[IO], Response[IO]]

implicit val decIR: Decoder[Option[AuthUser]] = deriveDecoder
implicit val encIR: Encoder[Option[AuthUser]] = deriveEncoder
implicit val decIR2: EntityDecoder[IO, Option[AuthUser]] =
  jsonOf[IO, Option[AuthUser]]

/*
Request[IO] => OptionT[IO, AuthUser]
~ IO[Option[AuthUser]]
 */

def userReq(af: AuthFunctions, r: Request[IO]): IO[Option[AuthUser]] =
  af.authorizeUserFromToken.run(r).map(_.toOption)

def authUser(
    af: AuthFunctions
): Kleisli[OptionT[IO, *], Request[IO], AuthUser] =
  Kleisli(r => OptionT(userReq(af, r)))

def middleware(af: AuthFunctions): AuthMiddleware[IO, AuthUser] =
  AuthMiddleware(authUser(af))
//    case req@GET -> Root / "get_user" => for {
//      res <- af.authorizeUserFromToken.run(req)
//      resp <- res match
//      case Right(au) => Ok(au.asJson)
//      case Left(error) => Forbidden(error)
//    } yield resp
def authedRoutes(dt: DataTools): AuthedRoutes[AuthUser, IO] =
  AuthedRoutes.of {
    case GET -> Root / "get_user" as user => {
      for {
        resp <- Ok(user.asJson)
      } yield resp
    }
    case GET -> Root / "ingredient_sets" as user => {
      for {
        ing <- for {
          is <- dt.getIngredientSets(user.id)
        } yield Results(is, "Ingredient Sets").asJson
        resp <- Ok(ing)
      } yield resp
    }

  }

def openRoutes(dt: DataTools, af: AuthFunctions) = {
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
    case req @ POST -> Root / "recipes_possible" =>
      for {
        isl <- req.as[Results[String]]
        j <- for {
          rfi <- dt.getRecipesForIngredients(isl.data)
        } yield Results(rfi, "Recipes").asJson
        resp <- Ok(j)
      } yield resp
    case req @ POST -> Root / "login"    => af.logIn.run(req)
    case req @ POST -> Root / "register" => af.register.run(req)
    case req @ GET -> Root / "example"   => Ok("HELLO!")
    case req @ GET -> Root / "example2"  => Ok(LoginResponse("hi").asJson)
  }
}

def jsonApp(ap: AppParams): Http4sApp = {
  val dt = DataTools(ap.db)
  val af = AuthFunctions(ap.auth, ap.db)
  openRoutes(dt, af) <+> middleware(af)(authedRoutes(dt))
}.orNotFound

//val allowedOrigin = Origin.Host(Uri.Scheme.http, Uri.RegName("localhost"), Some(8082))
//val allowedAll = CORS.policy.withAllowOriginAll

//val withMiddleWare = CORS.policy
//  .withAllowOriginHost(Set(allowedOrigin))
//  .withAllowCredentials(false)
//  .apply(jsonApp)

def server(
    ap: AppParams = AppParams(Settings.DevLocal.getSetup())
): Resource[IO, org.http4s.server.Server] = {
  val app = jsonApp(ap)
  val withMiddleWare = CORS.policy.withAllowOriginAll(app)

  EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(withMiddleWare)
    .build

}
