
lazy val root = (project in file("."))
  .settings(
    name := "snapshot-fetcher",
    version := "0.1",
    scalaVersion := "2.12.8",
    resolvers += Resolver.sonatypeRepo("releases"),
    fork in run := true,

    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9"),
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % "7.2.27",
      "org.scalaz" %% "scalaz-ioeffect" % "2.10.1",
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    )
  )
