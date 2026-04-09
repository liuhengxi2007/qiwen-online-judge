ThisBuild / scalaVersion := "3.3.3"
ThisBuild / organization := "com.example"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "qiwen-judger",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "io.circe" %% "circe-generic" % "0.14.9",
      "io.circe" %% "circe-parser" % "0.14.9",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "org.slf4j" % "slf4j-simple" % "2.0.13"
    ),
    Compile / run / mainClass := Some("Main"),
    Compile / run / fork := true
  )
