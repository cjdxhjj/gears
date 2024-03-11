package gears.async

import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeoutException
import gears.async.AsyncOperations.sleep

trait AsyncOperations:
  /** Suspends the current [[Async]] context for at least [[millis]] milliseconds. */
  def sleep(millis: Long)(using Async): Unit

object AsyncOperations:
  /** Suspends the current [[Async]] context for at least [[millis]] milliseconds.
    * @param millis
    *   The duration to suspend. Must be a positive integer.
    */
  inline def sleep(millis: Long)(using AsyncOperations, Async): Unit =
    summon[AsyncOperations].sleep(millis)

  /** Suspends the current [[Async]] context for at least [[millis]] milliseconds.
    * @param duration
    *   The duration to suspend. Must be positive.
    */
  inline def sleep(duration: FiniteDuration)(using AsyncOperations, Async): Unit =
    sleep(duration.toMillis)

/** Runs [[op]] with a timeout. When the timeout occurs, [[op]] is cancelled through the given [[Async]] context, and
  * [[TimeoutException]] is thrown.
  */
def withTimeout[T](timeout: FiniteDuration)(op: Async ?=> T)(using AsyncOperations, Async): T =
  Async.group:
    Async.select(
      Future(op).handle(_.get),
      Future(sleep(timeout)).handle: _ =>
        throw TimeoutException()
    )

/** Runs [[op]] with a timeout. When the timeout occurs, [[op]] is cancelled through the given [[Async]] context, and
  * [[None]] is returned.
  */
def withTimeoutOption[T](timeout: FiniteDuration)(op: Async ?=> T)(using AsyncOperations, Async): Option[T] =
  Async.group:
    Async.select(
      Future(op).handle(v => Some(v.get)),
      Future(sleep(timeout)).handle(_ => None)
    )
