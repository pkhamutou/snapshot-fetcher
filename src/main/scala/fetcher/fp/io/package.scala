package fetcher.fp

import scalaz.ioeffect.IO

package object io {

  trait Snapshot {
    def name: String
  }

  trait Peer {
    def name: String
    def fetch(path: String): IO[String, Snapshot]
  }

  trait Metric {
    def increment(name: String): IO[scalaz.ioeffect.Void, Unit]
  }

  trait Logger {

    def info(msg: String): IO[scalaz.ioeffect.Void, Unit] =
      IO.sync(println(s"[info] [${java.time.LocalTime.now}] $msg"))
  }

  object Metric {
    val failed: String    = "downloadFiled"
    val succeeded: String = "downloadSucceeded"
  }

}
