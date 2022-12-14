package com.chrisgoldammer.cocktails

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp

import com.chrisgoldammer.cocktails.*
import com.chrisgoldammer.cocktails.data.*
import com.chrisgoldammer.cocktails.data.types.*

object Main extends IOApp.Simple:
  val settings = getSettings()
  val dbSetup = settings.getOrElse(Settings.DevLocal).getSetup()
  val ap = AppParams(dbSetup)
  def run: IO[Unit] = server(ap).use(_ => IO.never).as(ExitCode.Success)

object DataSetupDevMain extends IOApp.Simple:
  def run: IO[Unit] = {
    val settings = getSettings()
    val dbSetup = settings.getOrElse(Settings.DevLocal).getSetup()
    val dt = DataTools(dbSetup)
    dt.setup()
  }

object DataSetupProdMain extends IOApp.Simple:
  def run: IO[Unit] = {
    val dbSetup = Settings.Prod.getSetup()
    val dt = DataTools(dbSetup)
    dt.setup(setupData=None)
  }
