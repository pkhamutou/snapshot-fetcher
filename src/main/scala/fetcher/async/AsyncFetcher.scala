package fetcher.async

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal

class AsyncFetcher(metric: Metric) {

  def fetch(paths: List[String], peers: List[Peer]): Future[List[Snapshot]] = {
    Future.traverse(paths.grouped(peers.length).flatMap(peers.zip(_)).toList) {
      case (peer, path) =>
        for {
          snapshot <- retry(path, peer :: peers.filterNot(_ == peer))
          _        <- metric.increment(Metric.succeeded)
        } yield snapshot
    }
  }

  private[async] def retry(path: String, peers: List[Peer]): Future[Snapshot] = peers match {
    case Nil => Future.failed[Snapshot](new Exception(s"failed fetching $path"))
    case peer :: xs =>
      peer.fetch(path).recoverWith {
        case NonFatal(_) => metric.increment(Metric.failed).flatMap(_ => retry(path, xs))
      }
  }
}
