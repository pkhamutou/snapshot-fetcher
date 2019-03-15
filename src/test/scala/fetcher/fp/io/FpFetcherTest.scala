package fetcher.fp.io

import org.scalatest.{FunSuite, Matchers}
import scalaz.ioeffect
import scalaz.ioeffect.{IO, RTS}

class FpFetcherTest extends FunSuite with Matchers with RTS {

  class PeerN extends Peer {
    override def fetch(path: String): IO[String, Snapshot] =
      if (path.length % 2 == 0) IO.fail(s"peer: failed for $path")
      else IO.sync(println(s"fetching $path")) *> IO.point(new Snapshot {
        override def name: String = path
      })
  }

  test("FpFetcher") {
    val peers = List(new PeerN, new PeerN, new PeerN)

    val metric = new Metric {
      override def increment(name: String): IO[ioeffect.Void, Unit] = IO.unit
    }

    val fetcher = new FpFetcher(metric)

    val io = fetcher.fetch(List("a", "bb", "ccc"), peers)

    unsafePerformIO(io)
  }
}
