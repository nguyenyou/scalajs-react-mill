package japgolly.scalajs.react.feature

import japgolly.scalajs.react.facade
import japgolly.scalajs.react.internal.FacadeExports.Key
import japgolly.scalajs.react.vdom._
import scala.scalajs.js

object ReactFragment {

  /** Unlike [[VdomArray]],
    *
    *   - This is immutable.
    *   - Elements may, but needn't have keys.
    *   - The result can be assigned a key.
    */
  def apply(ns: VdomNode*): VdomElement =
    create(null, ns*)

  /** Unlike [[VdomArray]],
    *
    *   - This is immutable.
    *   - Elements may, but needn't have keys.
    *   - The result can be assigned a key.
    */
  def withKey(key: Key)(ns: VdomNode*): VdomElement = {
    val jsKey: facade.React.Key = key
    val props = js.Dynamic.literal("key" -> jsKey.asInstanceOf[js.Any])
    create(props, ns*)
  }

  private def create(props: js.Object, ns: VdomNode*): VdomElement =
    VdomElement(
      facade.React
        .createElement(facade.React.Fragment, props, ns.map(_.rawNode)*)
    )
}
