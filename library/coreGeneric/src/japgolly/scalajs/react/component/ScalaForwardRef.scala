package japgolly.scalajs.react.component

import japgolly.scalajs.react.component.ScalaForwardRef._
import japgolly.scalajs.react.internal._
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, CtorType, PropsChildren, Ref, facade}
import scala.annotation.nowarn
import scala.scalajs.js

object ScalaForwardRef {
  type Component[P, R, CT[-p, +u] <: CtorType[p, u]] = JsForwardRef.ComponentWithRoot[P, R, CT, Unmounted[P, R], Box[P], CT, JsForwardRef.Unmounted[Box[P], R]]
  type Unmounted[P, R]                               = JsForwardRef.UnmountedWithRoot[P, R, Mounted, Box[P]]
  type Mounted                                       = JsForwardRef.Mounted
}

object ReactForwardRefInternals {

  sealed trait Dsl extends Any {
    protected type R
    protected type RefValue

    protected def create[P, C <: Children, CT[-p, +u] <: CtorType[p, u]]
      (render: (Box[P] & facade.PropsWithChildren, Option[R]) => VdomNode)
      (implicit s: CtorType.Summoner.Aux[Box[P], C, CT]): Component[P, RefValue, CT]

    final def apply(render: Option[R] => VdomNode): Component[Unit, RefValue, CtorType.Nullary] =
      create((_, r) => render(r))

    final def apply[P](render: (P, Option[R]) => VdomNode): Component[P, RefValue, CtorType.Props] =
      create((p, r) => render(p.unbox, r))

    final def withChildren[P](render: (P, PropsChildren, Option[R]) => VdomNode): Component[P, RefValue, CtorType.PropsAndChildren] =
      create((b, r) => render(b.unbox, PropsChildren(b.children), r))

    final def justChildren(render: (PropsChildren, Option[R]) => VdomNode): Component[Unit, RefValue, CtorType.Children] =
      create((b, r) => render(PropsChildren(b.children), r))
  }

  // extends AnyVal with Dsl makes scalac 2.11 explode
  final class ToJsComponent[P0 <: js.Object, S0 <: js.Object, RM <: Js.RawMounted[P0, S0]] private[component] (private val u: Unit) extends /*AnyVal with*/ Dsl {
    override protected type R = Ref.ToJsComponent[P0, S0, RM]
    override protected type RefValue = RM

    override protected def create[P, C <: Children, CT[-p, +u] <: CtorType[p, u]]
        (render: (Box[P] & facade.PropsWithChildren, Option[R]) => VdomNode)
        (implicit s: CtorType.Summoner.Aux[Box[P], C, CT]): Component[P, RefValue, CT] =
      ReactForwardRef.create[P, RefValue, C, CT]((p, r) => render(p, r.map(_.map(
        Js.mounted[P0, S0](_).withRawType[RM]
      ))))
  }

  // extends AnyVal with Dsl makes scalac 2.11 explode
  final class ToScalaComponent[P2, S, B] private[component] (private val u: Unit) extends /*AnyVal with*/ Dsl {
    override protected type R = Ref.ToScalaComponent[P2, S, B]
    override protected type RefValue = Scala.RawMounted[P2, S, B]

    override protected def create[P, C <: Children, CT[-p, +u] <: CtorType[p, u]]
        (render: (Box[P] & facade.PropsWithChildren, Option[R]) => VdomNode)
        (implicit s: CtorType.Summoner.Aux[Box[P], C, CT]): Component[P, RefValue, CT] =
      ReactForwardRef.create[P, RefValue, C, CT]((p, r) => render(p, r.map(_.map(_.mountedImpure))))
  }
}

object ReactForwardRef { outer =>
  import ReactForwardRefInternals._

  private[component] def create[P, R, C <: Children, CT[-p, +u] <: CtorType[p, u]]
      (render: (Box[P] & facade.PropsWithChildren, Option[Ref.Simple[R]]) => VdomNode)
      (implicit s: CtorType.Summoner.Aux[Box[P], C, CT]): Component[P, R, CT] = {

    val jsRender: js.Function2[Box[P] & facade.PropsWithChildren, facade.React.ForwardedRef[R], facade.React.Node] =
      (p: Box[P] & facade.PropsWithChildren, r: facade.React.ForwardedRef[R]) =>
        render(p, Ref.forwardedFromJs(r)).rawNode

    val rawComponent = facade.React.forwardRef(jsRender)

    JsForwardRef.force[Box[P], C, R](rawComponent)(s)
      .cmapCtorProps[P](Box(_))
      .mapUnmounted(_.mapUnmountedProps(_.unbox))
  }

  def apply[R](render: Option[Ref.Simple[R]] => VdomNode): Component[Unit, R, CtorType.Nullary] =
    create((_, r) => render(r))

  def apply[P, R](render: (P, Option[Ref.Simple[R]]) => VdomNode): Component[P, R, CtorType.Props] =
    create((p, r) => render(p.unbox, r))

  def withChildren[P, R](render: (P, PropsChildren, Option[Ref.Simple[R]]) => VdomNode): Component[P, R, CtorType.PropsAndChildren] =
    create((b, r) => render(b.unbox, PropsChildren(b.children), r))

  def justChildren[R](render: (PropsChildren, Option[Ref.Simple[R]]) => VdomNode): Component[Unit, R, CtorType.Children] =
    create((b, r) => render(PropsChildren(b.children), r))

  // ===================================================================================================================

  @inline def toJsComponent[F[_], A[_], P1, S1, CT1[-p, +u] <: CtorType[p, u], R <: Js.RawMounted[P0, S0], P0 <: js.Object, S0 <: js.Object, CT0[-p, +u] <: CtorType[p, u]]( c: Js.ComponentMapped[F, A, P1, S1, CT1, R, P0, S0, CT0]): ToJsComponent[P0, S0, R] =
    toJsComponent[P0, S0, R]

  def toJsComponent[P <: js.Object, S <: js.Object, R <: Js.RawMounted[P, S]]: ToJsComponent[P, S, R] =
    new ToJsComponent(())

  @inline def toScalaComponent[P, S, B, CT[-p, +u] <: CtorType[p, u]]( c: Scala.Component[P, S, B, CT]): ToScalaComponent[P, S, B] =
    toScalaComponent[P, S, B]

  def toScalaComponent[P, S, B]: ToScalaComponent[P, S, B] =
    new ToScalaComponent(())
}
