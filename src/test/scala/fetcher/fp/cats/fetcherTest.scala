package fetcher.fp.cats

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

import cats.Parallel
import cats.effect.IO._
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import org.scalatest.FunSuite

class fetcherTest extends FunSuite {

  case class Sp(name: String) extends Snapshot

  test("test Fetch with IO") {

    implicit val customPool = global

    implicit val timer: Timer[IO]     = IO.timer(customPool)
    implicit val cs: ContextShift[IO] = IO.contextShift(customPool)

    implicit val L: Logger[IO] = Logger.create[IO]

    implicit val M: Metric[IO] = new Metric[IO] {
      override def increment(name: String): IO[Unit] = IO.unit
    }

    class PeerF(override val name: String) extends Peer[IO] {
      override def fetch(path: String): IO[Snapshot] = {
        if (path.length == 2) IO.sleep(1.second) *> IO.raiseError(new Exception(s"failed on $path"))
        else {
          L.info(s"$name fetching $path on ${Thread.currentThread()}") *>
            IO.sleep(path.length.seconds) *>
            IO.sleep((path.length + 1).seconds) *>
            IO.delay(Sp(path)).flatMap(p => L.info(s"$name fetched $path on ${Thread.currentThread()}").map(_ => p))
        }
      }

      override def toString: String = s"PeerF($name)"
    }

    val peers = List(new PeerF("p1"), new PeerF("p2"), new PeerF("p3"))

    println(
      fetcher
        .fetch[IO, IO.Par](List("a", "bb", "ccc", "d"), peers)
        .attempt
        .unsafeRunSync()
    )

  }

  test("test Fetch with Future") {

    // IT'S A HACK! JUST FOR FT DEMONSTRATION PURPOSE

    implicit val futureParallel: Parallel[Future, Future] = Parallel.identity[Future]

    implicit val L: Logger[Future] = new Logger[Future] {
      override def info(msg: String): Future[Unit] = Future(println(s"[info] [${java.time.LocalTime.now}] $msg"))
    }

    implicit val M: Metric[Future] = new Metric[Future] {
      override def increment(name: String): Future[Unit] = Future.unit
    }

    def sleep(n: Int): Unit = Thread.sleep(n * 1000L)

    class PeerF(override val name: String) extends Peer[Future] {
      override def fetch(path: String): Future[Snapshot] = {
        if (path.length == 2) {
          for {
            _ <- Future.apply(sleep(1))
            e <- Future.failed[Snapshot](new Exception(s"failed on $path"))
          } yield e
        } else {
          for {
            _ <- L.info(s"$name fetching $path on ${Thread.currentThread()}")
            _ <- Future.apply(sleep(path.length))
            _ <- Future.apply(sleep(path.length + 1))
            p <- Future.apply(Sp(path))
            _ <- L.info(s"$name fetched $path on ${Thread.currentThread()}")
          } yield p
        }
      }

      override def toString: String = s"PeerF($name)"
    }

    val peers = List(new PeerF("p1"), new PeerF("p2"), new PeerF("p3"))

    println(Await.result(fetcher.fetch[Future, Future](List("a", "bb", "ccc", "d"), peers).attempt, 10.seconds))

  }
}
