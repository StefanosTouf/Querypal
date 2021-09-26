import Common._

import doobie.implicits._
import doobie.util.fragment.Fragment
import FragmentOperations.Commands
import FragmentOperations._
import FragmentOperations.Relation

/** The starting point of every query with A being the entity and B being the
  * Model of A (an object containing the fields).
  *
  * A subclass of Model[A] is required to preserve autocompletion.
  */

final class QueryBuilder[A, B <: Model[A]](model: B)(using
    meta: ModelMeta[A]
):
  val table = meta.table

  def select: Where[A, B] = Select(model)(
    Query(Commands.select, table, List[Argument]())
  ).select

  type BiRelation[B] = Relation[A, B] | Relation[B, A]

  def join[C: ModelMeta: BiRelation](
      toJoin: Model[C]
  ): Select[A, B] =
    Select(model)(
      Query(Commands.select, table, List[Argument](SqlOperations.joinOp[A, C]))
    )

  def delete: Where[A, B] = Select(model)(
    Query(Commands.delete, table, List[Argument]())
  ).select

  def insert: Insert[A] =
    Insert(Query(Commands.insert, table, List[Argument]()))

  def update(f: B => SetArgument): Set[A, B] =
    Set(model)(
      Query(Commands.update, table, List(Arguments.set, f(model)))
    )

object QueryStart:
  def apply[A: ModelMeta, B <: Model[A]](model: B): QueryBuilder[A, B] =
    new QueryBuilder(model)
