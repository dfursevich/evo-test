name := "evolution-gaming"

version := "0.1"

scalaVersion := "2.12.7"

lazy val akkaHttpVersion = "10.1.8"
lazy val akkaVersion    = "2.6.0-M2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
)
