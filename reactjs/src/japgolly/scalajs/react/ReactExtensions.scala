package japgolly.scalajs.react

import japgolly.scalajs.react.util.Effect.Sync

trait ReactExtensions {
  import ReactExtensions._

  @inline final implicit def ReactExt_OptionSync[F[_]](
      o: Option[F[Unit]]
  ): ReactExt_OptionSyncUnit[F] =
    new ReactExt_OptionSyncUnit(o)

}

object ReactExtensions {

  @inline implicit final class ReactExt_OptionSyncUnit[F[_]](
      private val o: Option[F[Unit]]
  ) extends AnyVal {

    /** Convenience for `.getOrElse(Callback.empty)` */
    @inline def getOrEmpty(implicit F: Sync[F]): F[Unit] =
      o.getOrElse(F.empty)
  }

  // I am NOT happy about this here... but it will do for now.

}
