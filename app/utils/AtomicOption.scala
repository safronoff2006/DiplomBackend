package utils

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

class AtomicOption[A](initial: Option[A]) {

  private val state: AtomicReference[Option[A]] = new  AtomicReference[Option[A]](initial)

  @tailrec
  private def storeState(optState: Option[A]): Unit = {
    val old = state.get()
    if (!state.compareAndSet(old,optState)) storeState(optState)
  }

  def setState(optState: Option[A]): Unit = storeState(optState)

  def getState: Option[A] = state.get()

}
