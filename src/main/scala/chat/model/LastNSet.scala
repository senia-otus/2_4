package chat.model

case class LastNSet[T](n: Int, nextSize: Int, next: Set[T], current: Set[T]) {
  def contains(t: T): Boolean = current.contains(t)
  def apply(t: T): Boolean = contains(t)

  def add(t: T): LastNSet[T] =
    if (next(t)) this
    else if (nextSize + 1 < n) LastNSet(n, nextSize + 1, next + t, current + t)
    else LastNSet(n, 0, Set.empty, next + t)
}

object LastNSet {
  def empty[T](n: Int): LastNSet[T] = LastNSet(n, 0, Set.empty, Set.empty)
}
