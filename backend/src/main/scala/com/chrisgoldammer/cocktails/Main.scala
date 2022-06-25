package com.chrisgoldammer.cocktails

import cats.effect.unsafe.implicits.global

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp.Simple:
  def run: IO[Unit] = server.use(_ => IO.never).as(ExitCode.Success)

