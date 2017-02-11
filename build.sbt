name := "stormy-haystack"

version := "1.0"

//scalaVersion := "2.12.1"
// TODO Figure out why we need to downgrade to 2.11.8 for dependencies to work
scalaVersion := "2.11.8"


resolvers += Resolver.mavenLocal

resolvers += "Kompics Releases" at "http://kompics.sics.se/maven/repository/"
resolvers += "Kompics Snapshots" at "http://kompics.sics.se/maven/snapshotrepository/"

libraryDependencies += "se.sics.kompics" %% "kompics-scala" % "0.9.2-SNAPSHOT"
libraryDependencies += "se.sics.kompics.basic" % "kompics-component-netty-network" % "0.9.2-SNAPSHOT"
libraryDependencies += "se.sics.kompics.basic" % "kompics-component-java-timer" % "0.9.2-SNAPSHOT"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "0.9.28"
libraryDependencies += "org.scala-lang.modules" %% "scala-pickling" % "0.10.1"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"