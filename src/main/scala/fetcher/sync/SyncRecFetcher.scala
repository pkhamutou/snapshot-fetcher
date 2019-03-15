package fetcher.sync

import scala.annotation.tailrec

class SyncRecFetcher(metric: Metric) {

  type Result[+A] = Either[String, A]

  /**
    * Fetches snapshots from provided URLs.
    * Each path is tried by each peer until it's successfully downloaded.
    * If all peers fail downloading at least one snapshot then return error message and interrupt further downloads.
    *
    * @param paths URLs to download snapshots from
    * @param peers Clients that can download snapshots
    * @return Error message if at least one snapshot was not fetched or the list of snapshots
    */
  def fetch(paths: List[String], peers: List[Peer]): Result[List[Snapshot]] = {

    @tailrec def run(paths: List[String], acc: Result[List[Snapshot]]): Result[List[Snapshot]] = (acc, paths) match {
      case (Right(snapshots), path :: xs) =>
        fetchOne(path, peers) match {
          case Left(error)     => Left(error)
          case Right(snapshot) => run(xs, Right(snapshot :: snapshots))
        }

      case (_, _) => acc
    }

    run(paths, Right(Nil))
  }

  private[sync] def fetchOne(path: String, peers: List[Peer]): Result[Snapshot] = {

    @tailrec def run(peers: List[Peer], acc: Result[Snapshot]): Result[Snapshot] = peers match {
      case Nil => acc
      case peer :: xs =>
        peer.fetch(path) match {
          case scala.util.Success(s) =>
            metric.increment(Metric.succeeded)
            Right(s)

          case _ =>
            metric.increment(Metric.failed)
            run(xs, acc)
        }
    }

    run(peers, Left(s"Failed downloading $path"))
  }

}
