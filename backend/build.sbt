val Http4sVersion = "0.23.6"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.6"
val MunitCatsEffectVersion = "1.0.6"
//lazy val DoobieVersion = "1.0-dbf0a76-20220527T014435Z-SNAPSHOT"
lazy val DoobieVersion = "1.0.0-RC2"
val ScalaVersion = "3.1.2"

lazy val root = (project in file("."))
  .settings(
    organization := "com.chrisgoldammer",
    name := "cocktails",
    version := "0.0.2",
    scalaVersion := ScalaVersion,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.scalameta" %% "munit" % MunitVersion % Test,
      "org.typelevel" %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
      "org.tpolecat" %% "doobie-hikari" % DoobieVersion
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-circe" % Http4sVersion
)

val circeVersion = "0.14.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

val tsecV = "0.4.0-M1"
libraryDependencies ++= Seq(
  "io.github.jmcardon" %% "tsec-common" % tsecV,
  "io.github.jmcardon" %% "tsec-password" % tsecV
)

// lazy val example = taskKey[Unit]("setupData")
// fullRunTask(example, Compile, "com.chrisgoldammer.cocktails.data.CallMe")

TaskKey[Unit]("setupData") := (runMain in Compile)
  .toTask(" com.chrisgoldammer.cocktails.data.CallMe")
  .value

scalacOptions ++= Seq( // use ++= to add to existing options
  "-Ykind-projector"
)
