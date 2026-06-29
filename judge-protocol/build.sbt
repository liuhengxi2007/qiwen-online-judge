ThisBuild / scalaVersion := "3.3.3"
ThisBuild / organization := "com.example"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "judge-protocol",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic" % "0.14.9",
      "io.circe" %% "circe-parser" % "0.14.9" % Test,
      "org.scalameta" %% "munit" % "1.1.1" % Test
    )
  )
