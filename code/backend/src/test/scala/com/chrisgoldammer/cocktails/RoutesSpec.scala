package com.chrisgoldammer.cocktails

import cats.effect.IO
import com.example.qs2.datastuff.IngredientResult
import org.http4s.*
import org.http4s.implicits.*
import munit.CatsEffectSuite

class IngredientsSpec extends CatsEffectSuite:

  test("Ingredients status code 200") {
    assertIO(retHelloWorld.map(_.status) ,Status.Ok)
  }

  test("HelloWorld returns hello world message") {
    assertIO(retHelloWorld.flatMap(_.as[IngredientResults]), )
  }

  private[this] val retHelloWorld: IO[Response[IO]] =
    val getHW = Request[IO](Method.GET, uri"/ingredients")
    val helloWorld = Cocktails.impl[IO]
    Qs2Routes.cocktailRoutes(helloWorld).orNotFound(getHW)
