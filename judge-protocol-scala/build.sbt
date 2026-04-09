ThisBuild / scalaVersion := "3.3.3"
ThisBuild / organization := "com.example"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "judge-protocol-scala",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic" % "0.14.9"
    )
  )
