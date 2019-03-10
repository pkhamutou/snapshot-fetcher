package fetcher.sync

import scala.util.{Try, Failure}

import org.scalatest.{FunSuite, Matchers}

class SyncFetcherTest extends FunSuite with Matchers {
  import SyncFetcherTest._

  test("test fetchOne") {

    val metric = TestMetric(0, 0)
    val fetcher = new SyncFetcher(metric)

    val result = fetcher.fetchOne("path1", List(TestPeer("path1")))

    result shouldBe Right(TestSnapshot("path1"))
    metric.succeeded shouldBe 1
    metric.failed shouldBe 0
  }

}

object SyncFetcherTest {

  case class TestSnapshot(name: String) extends Snapshot

  case class TestPeer(name: String) extends Peer {
    override def fetch(path: String): Try[Snapshot] = path match {
      case `name` => Try(TestSnapshot(name))
      case _ => Failure(new Exception(s"failed on $path"))
    }
  }

  case class TestMetric(var succeeded: Int, var failed: Int) extends Metric {
    override def increment(name: String): Unit = name match {
      case Metric.succeeded => succeeded += 1
      case Metric.failed => failed += 1
    }
  }

}
