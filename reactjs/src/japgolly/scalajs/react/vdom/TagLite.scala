package japgolly.scalajs.react.vdom

import scala.language.`3.0`

trait TagLite[Top <: TopNode] {
  final opaque type Tag[+N <: Top] = String

  @inline def apply[N <: Top](name: String): Tag[N] =
    name

  extension [N <: Top](self: Tag[N]) {
    @inline def name: String =
      self

  }

}

// =====================================================================================================================

object HtmlTagOf extends TagLite[HtmlTopNode]
export HtmlTagOf.{Tag => HtmlTagOf}

object SvgTagOf extends TagLite[SvgTopNode]
export SvgTagOf.{Tag => SvgTagOf}
