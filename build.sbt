name := "reactive-rest-mongo"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.4"

resolvers += Resolver.bintrayRepo("exaxisllc","SMD")

libraryDependencies ++= Seq(
  "org.scaldi" %% "scaldi-play" % "0.5.17"
  , "org.scaldi" %% "scaldi-akka" % "0.5.8"
  , "org.exaxis.smd" %% "smd-core" % "1.0.2"
  , "org.exaxis.smd" %% "smd-play" % "1.0.2"
  , "com.typesafe.play" % "play-json-joda_2.12" % "2.6.0"
  , "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
  , "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "2.1.1" % Test  // force a newer version
  , "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.4" %  Test
)
