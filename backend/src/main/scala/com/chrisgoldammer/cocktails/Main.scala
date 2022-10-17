package com.chrisgoldammer.cocktails

import com.chrisgoldammer.cocktails.data.{DBSetup, DataTools}
import cats.Applicative.*
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp.Simple:
  def run: IO[Unit] = server.use(_ => IO.never).as(ExitCode.Success)

object DataMain extends IOApp.Simple:
  def run: IO[Unit] = {
    val dt = DataTools(DBSetup())
    dt.setup()
  }
