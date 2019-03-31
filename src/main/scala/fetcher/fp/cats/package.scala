package fetcher.fp

import _root_.cats.effect.Sync

package object cats {

  trait Snapshot {
    def name: String
  }

  trait Peer[F[_]] {
    def name: String
    def fetch(path: String): F[Snapshot]
  }

  trait Metric[F[_]] {
    def increment(name: String): F[Unit]
  }

  object Metric {

    def apply[F[_]](implicit F: Metric[F]): Metric[F] = F

    val failed: String    = "downloadFiled"
    val succeeded: String = "downloadSucceeded"
  }

  trait Logger[F[_]] {

    def info(msg: String): F[Unit]
  }

  object Logger {

    def apply[F[_]](implicit F: Logger[F]): Logger[F] = F

    def create[F[_]: Sync]: Logger[F] = new Logger[F] {
      override def info(msg: String): F[Unit] =
        Sync[F].delay(println(s"[info] [${java.time.LocalTime.now}] $msg"))
    }
  }

}
