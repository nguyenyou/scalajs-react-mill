package japgolly.scalajs.react.vdom

import scala.scalajs.LinkingInfo.developmentMode

/** Represents a value that can be nested within a [[TagOf]]. This can be
  * another [[TagMod]], but can also be a CSS style or HTML attribute binding,
  * which will add itself to the node's attributes but not appear in the final
  * `children` list.
  */
trait TagMod {}

object TagMod {}
