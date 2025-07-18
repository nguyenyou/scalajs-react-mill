package japgolly.scalajs.react.component

import japgolly.scalajs.react.CtorType
import japgolly.scalajs.react.internal.{Box, Lens}
import japgolly.scalajs.react.util.DefaultEffects.{
  Async => DefaultA,
  Sync => DefaultS
}
import japgolly.scalajs.react.util.Effect.{Async, Dispatch, Id, UnsafeSync}
import scala.scalajs.js

object Scala {

  type Component[P, S, B, CT[-p, +u] <: CtorType[p, u]] =
    Js.ComponentWithRoot[P, CT, Unmounted[P, S, B], Box[P], CT, JsUnmounted[
      P,
      S,
      B
    ]]

  type Unmounted[P, S, B] =
    Js.UnmountedWithRoot[P, MountedImpure[P, S, B], Box[P], JsMounted[P, S, B]]
  type Mounted[F[_], A[_], P, S, B] = MountedRoot[F, A, P, S, B]
  type MountedImpure[P, S, B] = Mounted[Id, DefaultA, P, S, B]
  type MountedPure[P, S, B] = Mounted[DefaultS, DefaultA, P, S, B]
  type BackendScope[P, S] = Generic.MountedRoot[DefaultS, DefaultA, P, S]

  type JsComponent[P, S, B, CT[-p, +u] <: CtorType[p, u]] =
    Js.ComponentWithFacade[Box[P], Box[S], Vars[P, S, B], CT]
  type JsUnmounted[P, S, B] =
    Js.UnmountedWithFacade[Box[P], Box[S], Vars[P, S, B]]
  type JsMounted[P, S, B] = Js.MountedWithFacade[Box[P], Box[S], Vars[P, S, B]]

  type RawMounted[P, S, B] = Js.RawMounted[Box[P], Box[S]] & Vars[P, S, B]

  @js.native
  trait Vars[P, S, B] extends js.Object {
    var mountedImpure: MountedImpure[P, S, B]
    var mountedPure: MountedPure[P, S, B]
    var backend: B
  }

//  private[this] def sanityCheckCU[P, S, B](c: Component[P, S, B, CtorType.Void]): Unmounted[P, S, B] = c.ctor()
//  private[this] def sanityCheckUM[P, S, B](u: Unmounted[P, S, B]): Mounted[P, S, B] = u.renderIntoDOM(null)

  // ===================================================================================================================

  sealed trait MountedSimple[F[_], A[_], P, S, B]
      extends Generic.MountedSimple[F, A, P, S] {
    override type WithEffect[F2[_]] <: MountedSimple[F2, A, P, S, B]
    override type WithAsyncEffect[A2[_]] <: MountedSimple[F, A2, P, S, B]
    override type WithMappedProps[P2] <: MountedSimple[F, A, P2, S, B]
    override type WithMappedState[S2] <: MountedSimple[F, A, P, S2, B]

    // B instead of F[B] because
    // 1. Builder takes a MountedPure but needs immediate access to this.
    // 2. It never changes once initialised.
    // Note: Keep this is def instead of val because the builder sets it after creation.
    def backend: B
  }

  sealed trait MountedWithRoot[F[_], A[_], P1, S1, B, P0, S0]
      extends MountedSimple[F, A, P1, S1, B]
      with Generic.MountedWithRoot[F, A, P1, S1, P0, S0] {

    override final type Root = MountedRoot[F, A, P0, S0, B]
    override final type WithEffect[F2[_]] =
      MountedWithRoot[F2, A, P1, S1, B, P0, S0]
    override final type WithAsyncEffect[A2[_]] =
      MountedWithRoot[F, A2, P1, S1, B, P0, S0]
    override final type WithMappedProps[P2] =
      MountedWithRoot[F, A, P2, S1, B, P0, S0]
    override final type WithMappedState[S2] =
      MountedWithRoot[F, A, P1, S2, B, P0, S0]

    override final type Raw = RawMounted[P0, S0, B]

    val js: JsMounted[P0, S0, B]

    override final def displayName = js.displayName
    override final def backend = js.raw.backend
  }

  type MountedRoot[F[_], A[_], P, S, B] = MountedWithRoot[F, A, P, S, B, P, S]

  def mountedRoot[P, S, B](
      x: JsMounted[P, S, B]
  ): MountedRoot[Id, DefaultA, P, S, B] =
    new Template.MountedWithRoot[Id, DefaultA, P, S]()(UnsafeSync.id, DefaultA)
      with MountedRoot[Id, DefaultA, P, S, B] {

      override implicit def F = UnsafeSync.id
      override implicit def A = DefaultA
      override def root = this
      override val js = x
      override val raw = x.raw
      override def props = x.props.unbox
      override def propsChildren = x.propsChildren
      override def state = x.state.unbox
      override def getDOMNode = x.getDOMNode

      override def setState[G[_]](newState: S, callback: => G[Unit])(implicit
          G: Dispatch[G]
      ) =
        x.setState(Box(newState), callback)

      override def modState[G[_]](mod: S => S, callback: => G[Unit])(implicit
          G: Dispatch[G]
      ) =
        x.modState(s => Box(mod(s.unbox)), callback)

      override def modState[G[_]](mod: (S, P) => S, callback: => G[Unit])(
          implicit G: Dispatch[G]
      ) =
        x.modState((s, p) => Box(mod(s.unbox, p.unbox)), callback)

      override def setStateOption[G[_]](o: Option[S], callback: => G[Unit])(
          implicit G: Dispatch[G]
      ) =
        x.setStateOption(o.map(Box.apply), callback)

      override def modStateOption[G[_]](
          mod: S => Option[S],
          callback: => G[Unit]
      )(implicit G: Dispatch[G]) =
        x.modStateOption(s => mod(s.unbox).map(Box.apply), callback)

      override def modStateOption[G[_]](
          mod: (S, P) => Option[S],
          callback: => G[Unit]
      )(implicit G: Dispatch[G]) =
        x.modStateOption(
          (s, p) => mod(s.unbox, p.unbox).map(Box.apply),
          callback
        )

      override def forceUpdate[G[_]](callback: => G[Unit])(implicit
          G: Dispatch[G]
      ) =
        x.forceUpdate(callback)

      override type Mapped[F1[_], A1[_], P1, S1] =
        MountedWithRoot[F1, A1, P1, S1, B, P, S]
      override def mapped[F1[_], A1[_], P1, S1](
          mp: P => P1,
          ls: Lens[S, S1]
      )(implicit ft: UnsafeSync[F1], at: Async[A1]) =
        mappedM(this)(mp, ls)
    }

  private def mappedM[F[_], A[_], P2, S2, P1, S1, B, P0, S0](
      from: MountedWithRoot[Id, DefaultA, P1, S1, B, P0, S0]
  )(mp: P1 => P2, ls: Lens[S1, S2])(implicit
      ft: UnsafeSync[F],
      at: Async[A]
  ): MountedWithRoot[F, A, P2, S2, B, P0, S0] =
    new Template.MountedMapped[F, A, P2, S2, P1, S1, P0, S0](from)(mp, ls)
      with MountedWithRoot[F, A, P2, S2, B, P0, S0] {
      override def root = from.root.withEffect(ft).withAsyncEffect(at)
      override val js = from.js
      override val raw = from.raw
      override type Mapped[F3[_], A3[_], P3, S3] =
        MountedWithRoot[F3, A3, P3, S3, B, P0, S0]
      override def mapped[F3[_], A3[_], P3, S3](
          mp: P1 => P3,
          ls: Lens[S1, S3]
      )(implicit ft: UnsafeSync[F3], at: Async[A3]) =
        mappedM(from)(mp, ls)(ft, at)
    }

  def mountRaw[P, S, B](x: RawMounted[P, S, B]): MountedImpure[P, S, B] =
    mountedRoot(Js.mountedRoot(x))
}
