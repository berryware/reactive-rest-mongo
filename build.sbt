name := "reactive-rest-mongo"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"
lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  cache
  ,"org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23"
  , "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23"
  , "com.github.athieriot" %% "specs2-embedmongo" % "0.7.0" % Test
)

scalacOptions += "-feature"

