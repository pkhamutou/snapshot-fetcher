package fetcher.sync

class SyncFetcher(metric: Metric) {

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
  def fetch(paths: List[String], peers: List[Peer]): Result[List[Snapshot]] =
    paths.foldLeft[Result[List[Snapshot]]](Right(Nil)) {
      case (l @ Left(_), _)  => l
      case (Right(xs), path) => fetchOne(path, peers).map(_ :: xs)
    }

  private[sync] def fetchOne(path: String, peers: List[Peer]): Result[Snapshot] =
    peers.foldLeft[Result[Snapshot]](Left(s"Failed downloading $path")) {
      case (s @ Right(_), _) => s
      case (l @ Left(_), peer) =>
        peer
          .fetch(path)
          .fold(
            _ => { metric.increment(Metric.failed); l },
            sa => { metric.increment(Metric.succeeded); Right(sa) }
          )
    }
}
