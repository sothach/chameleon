name := """chameleon"""
organization := "org.anized"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  guice,
  "org.postgresql" % "postgresql" % "42.2.5",
  "com.typesafe.slick" %% "slick" % "3.3.2",
  "com.typesafe.play" %% "play-slick-evolutions" % "4.0.2",
  "com.typesafe.play" %% "play-slick" % "4.0.2",
  "com.spotify" % "docker-client" % "8.9.0",

  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
  "org.mockito" % "mockito-all" % "2.0.2-beta" % Test,
  "com.dimafeng" %% "testcontainers-scala" % "0.33.0" % Test,
  "org.testcontainers" % "postgresql" % "1.12.3" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.2" % Test
)

import play.sbt.routes.RoutesKeys.routesImport
routesImport += "model.JobSpecification"
routesImport += "conversions.Binders._"

coverageExcludedPackages := "<empty>;Reverse.*;router.*;controllers.javascript;play.api.*;views.html.*"

herokuAppName in Compile := "kid-chameleon"

enablePlugins(DockerPlugin)
val repoUser = "sothach"
val repoName = "chameleon"
val repo = "dscr.io"
javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)
maintainer in Docker := maintainer.toString()
dockerUsername := Some(repoUser)
dockerRepository := Some(s"$repo/$repoUser")
dockerAlias := DockerAlias(Some(repo),Some(repoUser),repoName,Some("latest"))
dockerUpdateLatest := true
dockerExposedPorts ++= Seq(9000)
dockerExposedVolumes := Seq("/opt/docker/logs")
