package org.querypal.logic

import doobie.implicits._
import Field._
import cats.kernel.Monoid
import cats.implicits._
import doobie.syntax.SqlInterpolator.SingleFragment.fromFragment
import org.querypal.conditions.JoinedSelect
import FragmentOperations.Argument
import Model._
import doobie.util.fragment.Fragment
import org.querypal.logic.DeriveModelMeta.ToDoobieString
import org.querypal.logic.QueryType._

object FragmentOperations:

  opaque type Argument = String

  opaque type Command <: Argument           = String
  opaque type ConditionOperator <: Argument = String
  opaque type Condition <: Argument         = String
  opaque type EqualsCondition <: Condition  = String
  opaque type SetArgument <: Argument       = String
  opaque type InsertArgument <: Argument    = String

  /** Operators used in the query-building pipeline. They enable type checking in the query
    * construction and SQL-like syntax
    */
  trait FieldOps[A]:
    extension [B](
        x: Column[B, A])(
        using
        meta: ModelMeta[B],
        toDoobie: ToDoobieString[A])
      def ===(y: A): EqualsCondition = y match
        case z: Int    =>
          s"${meta.table.name}" ++ s"." ++ s"${x.getName} = ${(z: Int)} "
        case z: String =>
          s"${meta.table.name}" ++ s"." ++ s"${x.getName} = '${(z: String)}' "

      def set(y: A): SetArgument =
        s"${x.getName} = ${toDoobie.toDoobieString(y)} "

  given FieldOps[Int] with
    extension [B](x: Column[B, Int])(using meta: ModelMeta[B])
      def >(y: Int): Condition =
        s"${meta.table.name}" ++ s"." + s"${x.getName} > $y "

      def <(y: Int): Condition =
        s"${meta.table.name}" ++ s"." + s"${x.getName} < $y "

  given FieldOps[String] with
    extension [B](x: Column[B, String])(using meta: ModelMeta[B])
      def like(y: String): Condition =
        meta.table.name ++ s".${x.getName} like '${y}' "

  given [A](using toDoobie: ToDoobieString[List[A]]): FieldOps[List[A]] with
    extension [B](x: Column[B, List[A]])(using meta: ModelMeta[B])
      def @>(elems: List[A]): Condition =
        meta.table.name ++ s".${x.getName} @> ${toDoobie.toDoobieString(elems)}"

      def like(y: String): Condition =
        s"""
           (
            0 < (
              SELECT COUNT(*) 
              FROM unnest(${meta.table.name}.${x.getName}) AS a
              WHERE a LIKE '$y'
            )
          )
        """

  extension (x: Argument) def ++(y: Argument) = x ++ y

  /** Helped methods that abstract the details of the sql syntax from the main pipeline
    */

  object GeneralOperators:
    val leftParen: Argument  = "( "
    val rightParen: Argument = ") "
    val comma: Argument      = ", "
    val dot: Argument        = "."
    val equals: Argument     = " = "

  object ConditionOperators:
    val and: ConditionOperator = " and "
    val or: ConditionOperator  = " or "

  object Commands:
    val update: Command = "update "
    val insert: Command = "insert into "
    val delete: Command = "delete from "
    val select: Command = "select * from "

  case class Table(private val nameStr: String):
    val name: Argument       = nameStr
    val nameAsString: String = nameStr

  /** A trait that enables any part of the pipeline to become a terminal step
    */

  object SqlOperations:
    import Argument._

    def commaSeparatedParened(content: List[String]): Argument =
      s"(" |+| content
        .drop(1)
        .fold(content.head)((x, y) =>
          x ++ (GeneralOperators.comma ++ y)) ++ GeneralOperators.rightParen

  object Arguments:
    val where: Argument     = " where "
    val values: Argument    = " values "
    val set: Argument       = " set "
    val innerJoin: Argument = " inner join "
    val on: Argument        = " on "

  object Argument:

    extension (str: String) def toArgument: Argument = str

    extension (str: Argument) def toString: String = str

    extension (content: List[Argument])
      def foldArgs: Argument =
        content.fold(Monoid[Argument].empty)(_ |+| _)

    given Monoid[Argument] with
      def empty                                                       = ""
      def combine(x: Argument, y: String | Argument): EqualsCondition = x + y

  val * : "* " = "* "
