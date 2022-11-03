package com.chrisgoldammer.cocktails

import com.chrisgoldammer.cocktails.data.*
import com.chrisgoldammer.cocktails.data.types.*
import com.chrisgoldammer.cocktails.*
import cats.Applicative.*
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp.Simple:
  val settings = getSettings()
  val dbSetup = settings.get.getSetup()
  val ap = AppParams(dbSetup)
  def run: IO[Unit] = server(ap).use(_ => IO.never).as(ExitCode.Success)

object DataMain extends IOApp.Simple:
  def run: IO[Unit] = {
    val settings = getSettings()
    val dbSetup = settings.get.getSetup()
    val dt = DataTools(dbSetup)
    dt.setup()
  }
