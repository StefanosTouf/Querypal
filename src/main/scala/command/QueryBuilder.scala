package command

import logic.Model._

import doobie.implicits._
import doobie.util.fragment.Fragment
import logic.FragmentOperations.Commands
import logic.FragmentOperations._
import conditions._
import arguments._
import logic.Join._
import logic.Query
import logic.Relation._

/** The starting point of every query with A being the entity and B being the
  * Model of A (an object containing the fields).
  *
  * A subclass of Model[A] is required to preserve autocompletion.
  */

final class QueryBuilder[A, B <: Model[A]](model: B)(using
    meta: ModelMeta[A]
):
  val table = meta.table

  def select: Where[A, B] & Joinable[A, B] = new WhereImpl[A, B](model)(
    Query(Commands.select, table)
  )

  type BiRelation[B] = Relation[A, B] | Relation[B, A]

  def delete: Where[A, B] = new WhereImpl[A, B](model)(
    Query(Commands.delete, table, List[Argument]())
  )

  def insert: Insert[A] =
    new Insert[A](Query(Commands.insert, table, List[Argument]()))

  def update(f: B => SetArgument): Set[A, B] =
    new Set[A, B](model)(
      Query(Commands.update, table, List(Arguments.set, f(model)))
    )

object QueryBuilder:
  def apply[A: ModelMeta, B <: Model[A]](model: B): QueryBuilder[A, B] =
    new QueryBuilder(model)
