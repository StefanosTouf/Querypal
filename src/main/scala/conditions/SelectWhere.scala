import Common._
import doobie.util.fragment.Fragment
import doobie.implicits._
import FragmentOperations._
import FragmentOperations.Arguments
import FragmentOperations.JoinableCompletable

/** Select and where are intermediary classes. They help preserve the SQL-like
  * syntax of the querypal dsl. A is the entity being queried and B the object
  * containing A's modeled fields.
  */

trait Where[A, B <: Model[A]] extends Completable {
  def apply(all: "* "): Joinable[A, B] & Completable

  def apply(f: B => Condition): Conditional[A, B] & Completable

  def complete: Argument

  def construct: Fragment
}

trait JoinedWhere[A, B, C <: Model[B]] extends Completable {
  def apply(all: "* "): JoinedJoinable[A, B, C] & Completable

  def apply(f: C => Condition): JoinedConditional[A, B, C] & Completable

  def complete: Argument

  def construct: Fragment
}

final class WhereImpl[A, B <: Model[A]](model: B)(query: Query)
    extends Where[A, B],
      Joinable[A, B] { self =>

  def join[C: ModelMeta: BiRelation, D <: Model[C]](
      toJoin: D
  ): JoinedSelect[A, C, D] =
    JoinedSelect(toJoin)(
      query.copy(
        joins = query.joins :+ SqlOperations.joinOp[A, C],
        conditions = query.conditions.addList
      )
    )

  def apply(all: "* "): Joinable[A, B] & Completable =
    new JoinableCompletable[A, B] {
      private val query = self.query

      def join[C: ModelMeta: BiRelation, D <: Model[C]](
          toJoin: D
      ): JoinedSelect[A, C, D] =
        JoinedSelect(toJoin)(
          query.copy(
            joins = query.joins :+ SqlOperations.joinOp[A, C],
            conditions = query.conditions.addList
          )
        )

      def complete: Argument = SqlOperations.complete(query)

      def construct: Fragment = SqlOperations.construct(query)
    }

  def apply(f: B => Condition): Conditional[A, B] & Completable =
    Conditional(model)(
      query.copy(conditions = query.conditions.addToLast(f(model)))
    )

  def complete: Argument = SqlOperations.complete(query)

  def construct: Fragment = SqlOperations.construct(query)
}

final class JoinedWhereImpl[A, B, C <: Model[B]](model: C)(query: Query)
    extends JoinedWhere[A, B, C],
      JoinedJoinable[A, B, C] { self =>

  def join[C: ModelMeta: BiRelation, D <: Model[C]](
      toJoin: D
  ): JoinedSelect[A, C, D] =
    JoinedSelect(toJoin)(
      query.copy(
        joins = query.joins :+ SqlOperations.joinOp[A, C],
        conditions = query.conditions.addList
      )
    )

  def apply(all: "* "): JoinedJoinable[A, B, C] & Completable =
    new JoinedJoinableCompletable[A, B, C] {
      private val query =
        self.query.copy(arguments = self.query.arguments.dropRight(1))

      def join[C: ModelMeta: BiRelation, D <: Model[C]](
          toJoin: D
      ): JoinedSelect[A, C, D] =
        JoinedSelect(toJoin)(
          query.copy(
            joins = query.joins :+ SqlOperations.joinOp[A, C],
            conditions = query.conditions.addList
          )
        )

      def complete: Argument = SqlOperations.complete(query)

      def construct: Fragment = SqlOperations.construct(query)
    }

  def apply(f: C => Condition): JoinedConditional[A, B, C] & Completable =
    JoinedConditional(model)(
      query.copy(conditions = query.conditions.addToLast(f(model)))
    )

  def complete: Argument = SqlOperations.complete(query)

  def construct: Fragment = SqlOperations.construct(query)
}

final class Select[A, B <: Model[A]](model: B)(query: Query) {
  def select: Where[A, B] & Joinable[A, B] =
    Where(model)(query)
}

final class JoinedSelect[A, B, C <: Model[B]](model: C)(query: Query) {
  def select: JoinedWhere[A, B, C] & JoinedJoinable[A, B, C] =
    JoinedWhere(model)(query)
}

object Where:
  def apply[A, B <: Model[A]](model: B)(query: Query): WhereImpl[A, B] =
    new WhereImpl[A, B](model)(query)

object JoinedWhere:
  def apply[A, B, C <: Model[B]](model: C)(
      query: Query
  ): JoinedWhereImpl[A, B, C] =
    new JoinedWhereImpl[A, B, C](model)(query)

object Select:
  def apply[A, B <: Model[A]](model: B)(query: Query) =
    new Select[A, B](model)(query)
