name := """oilserver"""
organization := "safronoff2006"
maintainer := "safronoff2006@gmail.com"
version := "1.0-SNAPSHOT"



lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  scalaVersion := "2.13.12",
  crossScalaVersions := Seq("2.13.12", "3.3.1"),


  libraryDependencies ++= Seq(
    guice,
    jdbc,
    evolutions,
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
    "org.postgresql" % "postgresql" % "42.6.0",
    "commons-codec" % "commons-codec" % "1.16.0",
    "com.typesafe.play" %% "play-slick" % "5.1.0",


    specs2 % Test,
    "com.typesafe.akka" %% "akka-stream-typed" %  play.core.PlayVersion.akkaVersion,

  ),

  scalacOptions ++= List("-encoding", "utf8", "-deprecation", "-feature", "-unchecked", "-Xfatal-warnings", "-language:implicitConversions"),
  javacOptions ++= List("-Xlint:unchecked", "-Xlint:deprecation", "-Werror")
)


  PlayKeys.devSettings += "play.server.http.idleTimeout" -> "3600000"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "safronoff2006.controllers._"
//TwirlKeys.templateImports += "safronoff2006.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "safronoff2006.binders._"
