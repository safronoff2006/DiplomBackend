name := """oilserver"""
organization := "safronoff2006"
maintainer := "safronoff2006@gmail.com"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  scalaVersion := "2.13.11",
    libraryDependencies ++= Seq(
    guice,
    jdbc,
    evolutions,
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
    "org.postgresql" % "postgresql" % "42.6.0",
    "org.playframework.anorm" %% "anorm" % "2.7.0",
    "commons-codec" % "commons-codec" % "1.16.0"

  ),
  scalacOptions ++= List("-encoding", "utf8", "-deprecation", "-feature", "-unchecked", "-Xfatal-warnings"),
  javacOptions ++= List("-Xlint:unchecked", "-Xlint:deprecation", "-Werror")
)


// Adds additional packages into Twirl
//TwirlKeys.templateImports += "safronoff2006.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "safronoff2006.binders._"
