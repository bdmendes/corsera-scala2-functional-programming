package objsets

import scala.annotation.tailrec

/**
 * A class to represent tweets.
 */
class Tweet(val user: String, val text: String, val retweets: Int) {
  override def toString: String =
    "User: " + user + "\n" +
    "Text: " + text + " [" + retweets + "]"
}

/**
 * This represents a set of objects of type `Tweet` in the form of a binary search
 * tree. Every branch in the tree has two children (two `TweetSet`s). There is an
 * invariant which always holds: for every branch `b`, all elements in the left
 * subtree are smaller than the tweet at `b`. The elements in the right subtree are
 * larger.
 *
 * Note that the above structure requires us to be able to compare two tweets (we
 * need to be able to say which of two tweets is larger, or if they are equal). In
 * this implementation, the equality / order of tweets is based on the tweet's text
 * (see `def incl`). Hence, a `TweetSet` could not contain two tweets with the same
 * text from different users.
 *
 *
 * The advantage of representing sets as binary search trees is that the elements
 * of the set can be found quickly. If you want to learn more you can take a look
 * at the Wikipedia page [1], but this is not necessary in order to solve this
 * assignment.
 *
 * [1] http://en.wikipedia.org/wiki/Binary_search_tree
 */
abstract class TweetSet extends TweetSetInterface {

  /**
   * This method takes a predicate and returns a subset of all the elements
   * in the original set for which the predicate is true.
   *
   * Question: Can we implement this method here, or should it remain abstract
   * and be implemented in the subclasses?
   * bdmendes: Since this is just a wrapper for filterAcc, just do this here.
   */
  def filter(p: Tweet => Boolean): TweetSet = filterAcc(p, new Empty())

  /**
   * This is a helper method for `filter` that propagates the accumulated tweets.
   */
  def filterAcc(p: Tweet => Boolean, acc: TweetSet): TweetSet

  /**
   * Returns a new `TweetSet` that is the union of `TweetSet`s `this` and `that`.
   *
   * Question: Should we implement this method here, or should it remain abstract
   * and be implemented in the subclasses?
   * bdmendes: Union with empty is special, so delegate this.
   */
  def union(that: TweetSet): TweetSet

  /**
   * Returns the tweet from this set which has the greatest retweet count.
   *
   * Calling `mostRetweeted` on an empty set should throw an exception of
   * type `java.util.NoSuchElementException`.
   *
   * Question: Should we implement this method here, or should it remain abstract
   * and be implemented in the subclasses?
   * bdmendes: There is no such thing on empty sets. Delegate.
   */
  def mostRetweeted: Tweet

  /**
   * Returns a list containing all tweets of this set, sorted by retweet count
   * in descending order. In other words, the head of the resulting list should
   * have the highest retweet count.
   *
   * Hint: the method `remove` on TweetSet will be very useful.
   * Question: Should we implement this method here, or should it remain abstract
   * and be implemented in the subclasses?
   * bdmendes: This simply uses mostRetweeted, so do this here.
   */
  def descendingByRetweet: TweetList = {
    @tailrec
    def loop(curr_set: TweetSet, acc: TweetList): TweetList = {
      if (curr_set.isEmpty) {
        acc
      } else {
        val best = curr_set.mostRetweeted
        loop(curr_set.remove(best), new Cons(best, acc))
      }
    }
    loop(this, Nil).reverse
  }

  /**
   * The following methods are already implemented
   */

  /**
   * Returns a new `TweetSet` which contains all elements of this set, and the
   * the new element `tweet` in case it does not already exist in this set.
   *
   * If `this.contains(tweet)`, the current set is returned.
   */
  def incl(tweet: Tweet): TweetSet

  /**
   * Returns a new `TweetSet` which excludes `tweet`.
   */
  def remove(tweet: Tweet): TweetSet

  /**
   * Tests if `tweet` exists in this `TweetSet`.
   */
  def contains(tweet: Tweet): Boolean

  /**
   * This method takes a function and applies it to every element in the set.
   */
  def foreach(f: Tweet => Unit): Unit

  def fold(f: (Tweet, TweetSet) => TweetSet, acc: TweetSet): TweetSet

  def isEmpty: Boolean

  def head: Tweet

  def size: Int
}

class Empty extends TweetSet {
  def filterAcc(p: Tweet => Boolean, acc: TweetSet): TweetSet = acc

  override def union(that: TweetSet): TweetSet = that

  /**
   * The following methods are already implemented
   */
  def contains(tweet: Tweet): Boolean = false

  def incl(tweet: Tweet): TweetSet = new NonEmpty(tweet, new Empty, new Empty)

  def remove(tweet: Tweet): TweetSet = this

  def foreach(f: Tweet => Unit): Unit = ()

  override def mostRetweeted: Tweet = throw new NoSuchElementException("No tweets in empty sets.")

  override def isEmpty: Boolean = true

  override def head: Tweet = throw new NoSuchElementException("No head on empty set.")

  override def size: Int = 0

  override def fold(f: (Tweet, TweetSet) => TweetSet, acc: TweetSet): TweetSet = acc
}

class NonEmpty(elem: Tweet, left: TweetSet, right: TweetSet) extends TweetSet {
  def filterAcc(p: Tweet => Boolean, acc: TweetSet): TweetSet = {
    val children = left.filterAcc(p, acc).union(right.filterAcc(p, acc))
    if (p(elem)) children.incl(elem) else children
  }

  override def head: Tweet = elem

  /**
   * The following methods are already implemented
   */

  def contains(x: Tweet): Boolean =
    if (x.text < elem.text) left.contains(x)
    else if (elem.text < x.text) right.contains(x)
    else true

  def incl(x: Tweet): TweetSet = {
    if (x.text < elem.text) new NonEmpty(elem, left.incl(x), right)
    else if (elem.text < x.text) new NonEmpty(elem, left, right.incl(x))
    else this
  }

  def remove(tw: Tweet): TweetSet =
    if (tw.text < elem.text) new NonEmpty(elem, left.remove(tw), right)
    else if (elem.text < tw.text) new NonEmpty(elem, left, right.remove(tw))
    else left.union(right)

  def foreach(f: Tweet => Unit): Unit = {
    f(elem)
    left.foreach(f)
    right.foreach(f)
  }

  override def union(that: TweetSet): TweetSet = {
    that.fold((t, acc) => acc.incl(t), this)
  }

  override def mostRetweeted: Tweet = {
    @tailrec
    def loop(set: TweetSet, best: Tweet): Tweet = {
      if (set.isEmpty) {
        best
      } else {
        loop(set.remove(set.head), if (set.head.retweets > best.retweets) set.head else best)
      }
    }
    loop(this, this.head)
  }

  override def isEmpty: Boolean = false

  override def size: Int = {
    1 + left.size + right.size
  }

  override def fold(f: (Tweet, TweetSet) => TweetSet, acc: TweetSet): TweetSet = {
    val afterHead = f(head, acc)
    val afterLeft = left.fold(f, afterHead)
    val afterRight = right.fold(f, afterLeft)
    afterRight
  }
}

trait TweetList {
  def head: Tweet
  def tail: TweetList
  def isEmpty: Boolean
  def foreach(f: Tweet => Unit): Unit =
    if (!isEmpty) {
      f(head)
      tail.foreach(f)
    }
  // bdmendes: Reverse the list.
  def reverse: TweetList
  def size: Int
}

object Nil extends TweetList {
  def head = throw new java.util.NoSuchElementException("head of EmptyList")
  def tail = throw new java.util.NoSuchElementException("tail of EmptyList")
  def isEmpty = true
  override def reverse = this
  override def size: Int = 0
}

class Cons(val head: Tweet, val tail: TweetList) extends TweetList {
  def isEmpty = false

  override def reverse: TweetList = {
    @tailrec
    def loop(l: TweetList, acc: TweetList): TweetList = {
      if (l.isEmpty) {
        acc
      } else {
        loop(l.tail, new Cons(l.head, acc))
      }
    }
    loop(this, Nil)
  }

  override def size: Int = tail.size + 1
}


object GoogleVsApple {
  val google = List("android", "Android", "galaxy", "Galaxy", "nexus", "Nexus")
  val apple = List("ios", "iOS", "iphone", "iPhone", "ipad", "iPad")

  lazy val googleTweets: TweetSet = TweetReader.allTweets.filter(t => google.exists(k => t.text.contains(k)))
  lazy val appleTweets: TweetSet = TweetReader.allTweets.filter(t => apple.exists(k => t.text.contains(k)))

  /**
   * A list of all tweets mentioning a keyword from either apple or google,
   * sorted by the number of retweets.
   */
  lazy val trending: TweetList = googleTweets.union(appleTweets).descendingByRetweet
}

object Main extends App {
  println("all size: " + TweetReader.allTweets.size)

  // Print the trending tweets
  GoogleVsApple.trending foreach println
  println(GoogleVsApple.trending.size)
}
