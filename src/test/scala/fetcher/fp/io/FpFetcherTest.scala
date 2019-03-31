package fetcher.fp.io

import org.scalatest.{FunSuite, Matchers}
import scalaz.-\/
import scalaz.ioeffect.{IO, RTS, Void}
import scala.concurrent.duration.DurationInt

class FpFetcherTest extends FunSuite with Matchers with RTS {

  override def defaultHandler[E]: Throwable => IO[E, Unit] =
    t => IO.sync(scala.Console.err.println(Thread.currentThread() + " " + t.getMessage))

  private val logger = new Logger {}

  case class SnapshotN(name: String) extends Snapshot

  class PeerN(override val name: String) extends Peer {
    override def fetch(path: String): IO[String, Snapshot] =
      if (path.length == 2) IO.fail(s"$name: failed for $path")
      else
        logger.info(s"$name fetching $path on ${Thread.currentThread()}").widenError[String] *>
          IO.sleep[String](path.length.seconds) *>
          IO.sync[String, Unit](Thread.sleep(path.length * 1000L)) *>
          IO.sync[String, Unit](Thread.sleep(path.length * 1000L)) *>
          IO.point(SnapshotN(path))
            .peek(
              _ =>
                logger
                  .info(s"$name fetched $path on ${Thread.currentThread()}")
                  .widenError[String]
            )
            .widen[Snapshot]
  }

  test("FpFetcher") {
    val peers = List(new PeerN("p1"), new PeerN("p2"), new PeerN("p3"))

    val metric = new Metric {
      override def increment(name: String): IO[Void, Unit] = IO.unit
    }

    val fetcher = new FpFetcher(metric, logger)

    val io = fetcher.fetch(List("a", "aaa", "bb", "ccc", "dd", "w", "ddd"), peers)

    unsafePerformIO(io.attempt) should (be(-\/("Interrupting due to failed fetching dd")) or be(
      -\/("Interrupting due to failed fetching bb")
    ))
  }
}
