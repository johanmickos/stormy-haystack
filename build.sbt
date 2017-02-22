name := "stormy-haystack"
scalaVersion := "2.11.8"

lazy val commonSettings = Seq(
    organization := "edu.kth",
    version := "1.0",
    // scalaVersion := "2.12.1"
    // TODO Figure out why we need to downgrade to 2.11.8 for dependencies to work
    scalaVersion := "2.11.8",
    resolvers ++= Seq(
        Resolver.mavenLocal,
        "Kompics Releases" at "http://kompics.sics.se/maven/repository/",
        "Kompics Snapshots" at "http://kompics.sics.se/maven/snapshotrepository/",
        "johnreed2 bintray" at "http://dl.bintray.com/content/johnreed2/maven" // For Intellij Scala Trace Debug
    ),
    libraryDependencies ++= Seq(
        "se.sics.kompics" %% "kompics-scala" % "0.9.2-SNAPSHOT",
        "se.sics.kompics.basic" % "kompics-component-netty-network" % "0.9.2-SNAPSHOT",
        "se.sics.kompics.basic" % "kompics-component-java-timer" % "0.9.2-SNAPSHOT",
        "se.sics.kompics.simulator" % "core" % "0.9.2-SNAPSHOT",
        "ch.qos.logback" % "logback-classic" % "0.9.28",
        "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
        "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
        "com.github.johnreedlol" %% "scala-trace-debug" % "3.0.6" // For Intellij Scala Trace Debug
    )
)

lazy val common = (project in file("common")) settings commonSettings
lazy val server = (project in file("server")) dependsOn common settings commonSettings
lazy val client = (project in file("client")) dependsOn common settings commonSettings
lazy val simulation = (project in file("simulation")) dependsOn common dependsOn server dependsOn client settings commonSettings
