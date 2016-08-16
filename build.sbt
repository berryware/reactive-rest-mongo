name := "reactive-rest-mongo"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"
lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  cache
  , "org.reactivemongo" %% "reactivemongo" % "0.11.14"
	, "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14"
	, "org.reactivemongo" %% "reactivemongo-play-json" % "0.11.14"
  , "org.scaldi" %% "scaldi-play" % "0.5.15"
  , "org.scaldi" %% "scaldi-akka" % "0.5.7"
  , "com.github.athieriot" %% "specs2-embedmongo" % "0.7.0" % Test
  , "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % Test
  , "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.2" %  Test
)

scalacOptions += "-feature"

