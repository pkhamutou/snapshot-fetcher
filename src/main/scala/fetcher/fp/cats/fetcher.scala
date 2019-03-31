package fetcher.fp.cats

import cats.instances.list._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.parallel._
import cats.{ApplicativeError, MonadError, Parallel}

object fetcher {

  /**
    * Fetches in parallel and kills all fibers if at least one path failed.
    * G[_] is required for cats parTraverse.
    */
  def fetch[F[_]: Logger: Metric: MonadError[?[_], Throwable]: Parallel[?[_], G], G[_]](
    paths: List[String],
    peers: List[Peer[F]]
  ): F[List[Snapshot]] = {

    val groups = paths
      .grouped(peers.length)
      .flatMap(peers.zip(_))
      .toList

    groups.parTraverse[F, G, Snapshot] {
      case (peer, path) => retry[F](path, peer :: peers.filterNot(_ == peer))
    }
  }

  def retry[F[_]: Metric: Logger: ApplicativeError[?[_], Throwable]](path: String, peers: List[Peer[F]]): F[Snapshot] =
    peers match {
      case Nil => ApplicativeError[F, Throwable].raiseError(new Exception(s"failed fetching $path"))
      case peer :: tail =>
        peer
          .fetch(path)
          .handleErrorWith(
            _ =>
              Metric[F].increment(Metric.failed) *>
                Logger[F].info(s"$peer failed fetching $path on ${Thread.currentThread()}, retrying...") *>
                retry(path, tail)
          )
    }
}
