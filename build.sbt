name := "reactive-rest-mongo"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.10.0"
  , "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2"
)


play.Project.playScalaSettings
