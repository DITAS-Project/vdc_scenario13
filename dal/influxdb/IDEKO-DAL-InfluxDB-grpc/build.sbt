
name := "ideko-dal-influxdb-grpc"

organization := "com.ditas"


version := "0.1"

scalaVersion := "2.12.6"

resolvers += Resolver.mavenLocal


PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.5.3",
  "joda-time" % "joda-time" % "2.9.9",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.9.8",
  "org.yaml" % "snakeyaml" % "1.11",
  "org.scalaj" %% "scalaj-http" % "2.4.1",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
)

//libraryDependencies ~= { _.map(_.exclude("com.fasterxml.jackson.module", "jackson-module-scala_2.11")) }

// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-scala
//libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.2"

libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-log4j12")) }
libraryDependencies ~= { _.map(_.exclude("log4j", "log4j")) }

enablePlugins(JavaAppPackaging)


assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case n if n.contains("services") => MergeStrategy.concat
  case n if n.startsWith("reference.conf") => MergeStrategy.concat
  case n if n.endsWith(".conf") => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

