
lazy val root = (project in file("."))
  .settings(
    name := "snapshot-fetcher",
    version := "0.1",
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % "7.2.27",
      "org.scalaz" %% "scalaz-ioeffect" % "2.10.1",
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    )
  )
