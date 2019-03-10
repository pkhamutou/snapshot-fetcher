package fetcher

import scala.util.Try

package object sync {

  trait Peer {
    def fetch(path: String): Try[Snapshot]
  }

  trait Snapshot {
    def name: String
  }

  trait Metric {
    def increment(name: String): Unit
  }

  object Metric {
    val failed: String = "downloadFiled"
    val succeeded: String = "downloadSucceeded"
  }

}
