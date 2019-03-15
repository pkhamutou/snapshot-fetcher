package fetcher.fp

import scalaz.ioeffect.IO

package object io {

  trait Snapshot {
    def name: String
  }

  trait Peer {
    def fetch(path: String): IO[String, Snapshot]
  }

  trait Metric {
    def increment(name: String): IO[scalaz.ioeffect.Void, Unit]
  }

  object Metric {
    val failed: String = "downloadFiled"
    val succeeded: String = "downloadSucceeded"
  }

}
