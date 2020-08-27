package chat.model

case class LastNSeq[+A](n: Int, nextSize: Int, next: List[A], current: List[A]) {
  lazy val lastN: Seq[A] = current.view.take(n).toVector

  def +[B >: A](t: B): LastNSeq[B] = add(t)

  def add[B >: A](t: B): LastNSeq[B] =
    if (nextSize + 1 < n) LastNSeq(n, nextSize + 1, t :: next, t :: current)
    else LastNSeq(n, 0, Nil, t :: next)
}

object LastNSeq {
  def empty(n: Int): LastNSeq[Nothing] = LastNSeq(n, 0, Nil, Nil)
}