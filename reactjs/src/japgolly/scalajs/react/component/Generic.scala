package japgolly.scalajs.react.component

import japgolly.scalajs.react.internal.FacadeExports.Key
import japgolly.scalajs.react.internal.Profunctor
import japgolly.scalajs.react.util.DefaultEffects.{
  Async => DefaultA,
  Sync => DefaultS
}
import japgolly.scalajs.react.util.Effect.{Dispatch, Id}
import japgolly.scalajs.react.util.JsUtil
import japgolly.scalajs.react.{
  ComponentDom,
  CtorType,
  PropsChildren,
  StateAccess,
  facade,
  vdom
}
import scala.scalajs.js
import scala.scalajs.js.|

object Generic {

  @inline implicit def toComponentCtor[P, CT[-p, +u] <: CtorType[p, u], U](
      c: ComponentSimple[P, CT, U]
  ): CT[P, U] =
    c.ctor
  // TODO: Remove after https://github.com/lampepfl/dotty/issues/12216
  @inline implicit def toComponentCtorC[P, U](
      c: ComponentSimple[P, CtorType.Children, U]
  ): CtorType.Children[P, U] = c.ctor
  @inline implicit def toComponentCtorN[P, U](
      c: ComponentSimple[P, CtorType.Nullary, U]
  ): CtorType.Nullary[P, U] = c.ctor
  @inline implicit def toComponentCtorP[P, U](
      c: ComponentSimple[P, CtorType.Props, U]
  ): CtorType.Props[P, U] = c.ctor
  @inline implicit def toComponentCtorPC[P, U](
      c: ComponentSimple[P, CtorType.PropsAndChildren, U]
  ): CtorType.PropsAndChildren[P, U] = c.ctor

  trait ComponentRaw {
    type Raw <: js.Any
    val raw: Raw
    def displayName: String
    def mapRaw(f: Raw => Raw): ComponentRaw
  }

  trait ComponentSimple[P, CT[-p, +u] <: CtorType[p, u], U]
      extends ComponentRaw {
    final type Props = P
    final type Unmounted = U

    def cmapCtorProps[P2](f: P2 => P): ComponentSimple[P2, CT, U]
    def mapUnmounted[U2](f: U => U2): ComponentSimple[P, CT, U2]
    def mapCtorType[CT2[-p, +u] <: CtorType[p, u]](f: CT[P, U] => CT2[P, U])(
        implicit pf: Profunctor[CT2]
    ): ComponentSimple[P, CT2, U]

    val ctor: CT[P, U]
    implicit def ctorPF: Profunctor[CT]

    override def mapRaw(f: Raw => Raw): ComponentSimple[P, CT, U]

  }

  trait ComponentWithRoot[P1, CT1[-p, +u] <: CtorType[p, u], U1, P0, CT0[
      -p,
      +u
  ] <: CtorType[p, u], U0]
      extends ComponentSimple[P1, CT1, U1] {
    type Root <: ComponentRoot[P0, CT0, U0]
    def root: Root

    override def mapRaw(
        f: Raw => Raw
    ): ComponentWithRoot[P1, CT1, U1, P0, CT0, U0]
    override def cmapCtorProps[P2](
        f: P2 => P1
    ): ComponentWithRoot[P2, CT1, U1, P0, CT0, U0]
    override def mapUnmounted[U2](
        f: U1 => U2
    ): ComponentWithRoot[P1, CT1, U2, P0, CT0, U0]
    override def mapCtorType[CT2[-p, +u] <: CtorType[p, u]](
        f: CT1[P1, U1] => CT2[P1, U1]
    )(implicit pf: Profunctor[CT2]): ComponentWithRoot[P1, CT2, U1, P0, CT0, U0]
  }

  type ComponentRoot[P, CT[-p, +u] <: CtorType[p, u], U] =
    ComponentWithRoot[P, CT, U, P, CT, U]

  // ===================================================================================================================

  type Unmounted[P, M] = UnmountedSimple[P, M]

  trait UnmountedRaw {
    type Raw <: facade.React.Element
    val raw: Raw
    def displayName: String

    final def vdomElement: vdom.VdomElement =
      vdom.VdomElement(raw)
  }

  implicit def unmountedRawToVdomElement(u: UnmountedRaw): vdom.VdomElement =
    u.vdomElement

  trait UnmountedSimple[P, M] extends UnmountedRaw {
    final type Props = P
    final type Mounted = M
    type Ref

    def mapUnmountedProps[P2](f: P => P2): UnmountedSimple[P2, M]
    def mapMounted[M2](f: M => M2): UnmountedSimple[P, M2]

    def key: Option[Key]
    def ref: Option[Ref]
    def props: Props
    def propsChildren: PropsChildren

    val mountRaw: facade.React.ComponentUntyped => M // TODO Do better

    final def mountRawOrNull(c: facade.React.ComponentUntyped | Null): M =
      if (c == null) null.asInstanceOf[M] else mountRaw(JsUtil.notNull(c))

    @deprecated(
      "Use ReactDOM.createRoot and root.render instead",
      "2.2.0 / React v18"
    )
    def renderIntoDOM(container: facade.ReactDOM.Container): Mounted =
      mountRaw(facade.ReactDOM.render(raw, container))

    def renderIntoDOM[G[_]](
        container: facade.ReactDOM.Container,
        callback: => G[Unit]
    )(implicit G: Dispatch[G]): Mounted =
      mountRaw(facade.ReactDOM.render(raw, container, G.dispatchFn(callback)))
  }

  trait UnmountedWithRoot[P1, M1, P0, M0] extends UnmountedSimple[P1, M1] {
    type Root <: UnmountedRoot[P0, M0]
    def root: Root

    override def mapUnmountedProps[P2](
        f: P1 => P2
    ): UnmountedWithRoot[P2, M1, P0, M0]
    override def mapMounted[M2](f: M1 => M2): UnmountedWithRoot[P1, M2, P0, M0]
  }

  type UnmountedRoot[P, M] = UnmountedWithRoot[P, M, P, M]

  // ===================================================================================================================

  type Mounted[F[_], A[_], P, S] = MountedSimple[F, A, P, S]
  type MountedPure[P, S] = MountedSimple[DefaultS, DefaultA, P, S]
  type MountedImpure[P, S] = MountedSimple[Id, DefaultA, P, S]

  trait MountedRaw {
    type Raw <: facade.React.ComponentUntyped // TODO Do better
    val raw: Raw
    def displayName: String
  }

  trait MountedSimple[F[_], A[_], P, S]
      extends MountedRaw
      with StateAccess[F, A, S]
      with StateAccess.WriteWithProps[F, A, P, S] {
    final type Props = P

    override type WithEffect[F2[_]] <: MountedSimple[F2, A, P, S]
    override type WithAsyncEffect[A2[_]] <: MountedSimple[F, A2, P, S]
    override type WithMappedState[S2] <: MountedSimple[F, A, P, S2]
    type WithMappedProps[P2] <: MountedSimple[F, A, P2, S]
    def mapProps[P2](f: P => P2): WithMappedProps[P2]

    def getDOMNode: F[ComponentDom]
    def props: F[Props]
    def propsChildren: F[PropsChildren]

    def forceUpdate[G[_]](callback: => G[Unit])(implicit
        G: Dispatch[G]
    ): F[Unit]
    final def forceUpdate: F[Unit] = forceUpdate(DefaultS.empty)
  }

  trait MountedWithRoot[F[_], A[_], P1, S1, P0, S0]
      extends MountedSimple[F, A, P1, S1] {
    type Root <: MountedRoot[F, A, P0, S0]
    def root: Root

    override type WithEffect[F2[_]] <: MountedWithRoot[F2, A, P1, S1, P0, S0]
    override type WithAsyncEffect[A2[_]] <: MountedWithRoot[
      F,
      A2,
      P1,
      S1,
      P0,
      S0
    ]
    override type WithMappedState[S2] <: MountedWithRoot[F, A, P1, S2, P0, S0]
    override type WithMappedProps[P2] <: MountedWithRoot[F, A, P2, S1, P0, S0]
  }

  type MountedRoot[F[_], A[_], P, S] = MountedWithRoot[F, A, P, S, P, S]
}
