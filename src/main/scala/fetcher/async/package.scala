package fetcher

import scala.concurrent.Future

package object async {

  trait Peer {
    def fetch(path: String): Future[Snapshot]
  }

  trait Snapshot {
    def name: String
  }

  trait Metric {
    def increment(name: String): Future[Unit]
  }

  object Metric {
    val failed: String = "downloadFiled"
    val succeeded: String = "downloadSucceeded"
  }
}
