package fetcher.fp.io

import scala.language.higherKinds

import scalaz.ioeffect._
import scalaz.ioeffect.IO._
import scalaz.std.list._
import scalaz.syntax.traverse._

class FpFetcher(metric: Metric, logger: Logger) {

  private[io] def buildGroups(paths: List[String], peers: List[Peer]): List[(Peer, String)] =
    paths
      .grouped(peers.length)
      .flatMap(peers.zip(_))
      .toList

  /**
    * Does not interrupt all fibers on failure due to the way parTraverse is implemented (as a sequence of IO.par calls)
    */
  def fetch0(paths: List[String], peers: List[Peer]): IO[String, List[Snapshot]] = {
    buildGroups(paths, peers).parTraverse[IO[String, ?], Snapshot] {
      case (peer, path) => retry(path, peer :: peers.filterNot(_ == peer))
    }
  }

  /**
    * Interrupts all fibers if at least one fails.
    */
  def fetch(paths: List[String], peers: List[Peer]): IO[String, List[Snapshot]] = {

    for {
      fibers <- buildGroups(paths, peers).parTraverse[IO[String, ?], Fiber[String, Snapshot]] {
                 case (peer, path) => retry(path, peer :: peers.filterNot(_ == peer)).fork[String]
               }
      snapshots <- fibers
                    .parTraverse[IO[String, ?], Snapshot](joinOrInterruptAll(_, fibers))
                    .run[String]
                    .flatMap {
                      case ExitResult.Completed(value) => IO.now[String, List[Snapshot]](value)
                      case ExitResult.Failed(e)        => IO.fail[String, List[Snapshot]](e)
                      case ExitResult.Terminated(e)    => IO.fail[String, List[Snapshot]](e.getMessage)
                    }
    } yield snapshots
  }

  private[io] def joinOrInterruptAll[A](fiber: Fiber[String, A], fibers: List[Fiber[String, A]]): IO[String, A] =
    fiber.join.redeem[String, A](
      err =>
        fibers.parTraverse[IO[String, ?], Unit](
          _.interrupt[String](new InterruptedException(s"Interrupting due to $err"))
        ) *> IO.fail(err),
      IO.now
    )

  private[io] def retry(path: String, peers: List[Peer]): IO[String, Snapshot] = peers match {
    case Nil => IO.fail(s"failed fetching $path")
    case peer :: xs =>
      peer
        .fetch(path)
        .redeem[String, Snapshot](
          _ => metric.increment(Metric.failed).widenError[String] *> retry(path, xs),
          IO.now
        )
  }
}
