package fetcher.fp.io

import scala.language.higherKinds

import scalaz.{-\/, \/-}
import scalaz.ioeffect._
import scalaz.ioeffect.IO._
import scalaz.std.list._
import scalaz.syntax.traverse._

class FpFetcher(metric: Metric) {
  def fetch(paths: List[String], peers: List[Peer]): IO[String, List[Snapshot]] =
    paths
      .grouped(peers.length)
      .flatMap(peers.zip(_))
      .toList
      .traverse[IO[String, ?], Snapshot] {
        case (peer, path) => retry(path, peer :: peers.filterNot(_ == peer))
      }

  def retry(path: String, peers: List[Peer]): IO[String, Snapshot] = peers match {
    case Nil => IO.fail(s"failed fetching $path")
    case peer :: xs =>
      peer
        .fetch(path)
        .fork
        .flatMap(_.join.attempt.flatMap {
          case -\/(e) => metric.increment(Metric.failed).widenError[String] *> retry(path, xs)
          case \/-(s) => IO.now(s)
        })
  }
}
