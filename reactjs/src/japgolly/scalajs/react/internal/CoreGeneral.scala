package japgolly.scalajs.react.internal

import japgolly.scalajs.react._
import japgolly.scalajs.react.util.Effect.Id
import japgolly.scalajs.react.util.{DomUtil, Effect}
import scala.scalajs.js

object CoreGeneral extends CoreGeneral

trait CoreGeneral
    extends ReactEventTypes
       with ReactExtensions
       with DomUtil
       with FacadeExports 
       with hooks.all {

  import japgolly.scalajs.react.util.DefaultEffects._

  final type StateAccessPure[S] = StateAccess[Sync, Async, S]
  final type StateAccessImpure[S] = StateAccess[Id, Async, S]

  final type SetStateFnPure[S] = SetStateFn[Sync, Async, S]
  final type SetStateFnImpure[S] = SetStateFn[Id, Async, S]

  final type ModStateFnPure[S] = ModStateFn[Sync, Async, S]
  final type ModStateFnImpure[S] = ModStateFn[Id, Async, S]

  final type ModStateWithPropsFnPure[P, S] = ModStateWithPropsFn[Sync, Async, P, S]
  final type ModStateWithPropsFnImpure[P, S] = ModStateWithPropsFn[Id, Async, P, S]

  final val ReactEffect = Effect

}

abstract class CoreGeneralF[F[_]](implicit F: Effect.Sync[F]) extends CoreGeneral {

  final lazy val preventDefault: ReactEvent => F[Unit] =
    e => F.delay { e.preventDefault() }

  final lazy val stopPropagation: ReactEvent => F[Unit] =
    e => F.delay { e.stopPropagation() }

  final lazy val preventDefaultAndStopPropagation: ReactEvent => F[Unit] =
    e => F.delay {
      e.preventDefault()
      e.stopPropagation()
    }
}
