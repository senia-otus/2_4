name := "chat cluster"
scalaVersion := "2.13.1"

val akkaVersion = "2.6.8"
val managementVersion = "1.0.8"
val zioVersion = "1.0.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.lightbend.akka.management" %% "akka-management" % managementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % managementVersion,
  "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.5.2",
  "org.postgresql" % "postgresql" % "42.2.9",
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion
)



libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
