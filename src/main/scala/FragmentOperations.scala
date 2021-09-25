import doobie.implicits._
import doobie.util.fragment.Fragment
import Common._
import cats.kernel.Monoid
import cats.implicits._
import FragmentOperations.PrimaryKey
import javax.management.relation.Relation
import doobie.syntax.SqlInterpolator.SingleFragment.fromFragment
import scala.annotation.targetName

object FragmentOperations:
  opaque type Command = Fragment

  opaque type Argument = Fragment

  opaque type ConditionOperator <: Argument = Fragment
  opaque type Condition <: Argument         = Fragment
  opaque type EqualsCondition <: Condition  = Fragment
  opaque type SetArgument <: Argument       = Fragment
  opaque type InsertArgument <: Argument    = Fragment

  extension (content: List[Fragment])
    def foldFragments =
      content.fold(Monoid[Fragment].empty)(_ |+| _)

  sealed trait Field[+A, B]:
    val name: Fragment
  case class Column[A, B](name: Fragment)        extends Field[A, B]
  case class ForeignKey[A, B, C](name: Fragment) extends Field[A, B]

  case class PrimaryKey[A, B](field: Field[A, B])

  trait Relation[A, B](from: Field[?, A])(using
      fromMeta: ModelMeta[A],
      toMeta: ModelMeta[B]
  ):
    val joinCondition: Argument =
      fromMeta.table.name ++ sql"." ++ from.name ++ fr"=" ++ toMeta.table.name ++ sql"." ++ toMeta.primaryKey.field.name

  trait FieldOps[A]:
    extension [B](x: Field[A, B])(using meta: ModelMeta[B])
      def ===(y: A): EqualsCondition = y match
        case z: Int =>
          sql"${meta.table.name}" ++ sql"." ++ fr"${x.name} = ${(z: Int)}"
        case z: String =>
          sql"${meta.table.name}" ++ sql"." ++ fr"${x.name} = ${(z: String)}"

      def set(y: A): SetArgument = y match
        case z: Int    => sql"${x.name} = ${z: Int}"
        case z: String => sql"${x.name} = ${z: String}"

  given FieldOps[Int] with
    extension [B](x: Field[Int, B])(using meta: ModelMeta[B])
      def >(y: Int): Condition =
        sql"${meta.table.name}" ++ sql"." ++ fr"${x.name} > $y"

      def <(y: Int): Condition =
        sql"${meta.table.name}" ++ sql"." ++ fr"${x.name} < $y"

  given FieldOps[String] with {
    extension [B](x: Field[String, B])(using meta: ModelMeta[B])
      def like(y: String): Condition =
        meta.table.name ++ fr".${x.name} like ${y}"
  }

  object SqlOperations:
    def commaSeparatedParened(content: List[Fragment]): Argument =
      sql"(" |+| content
        .drop(1)
        .fold(content.head)((x, y) =>
          x ++ (GeneralOperators.comma ++ y)
        ) ++ GeneralOperators.rightParen

    def joinOp[A, B](using
        relation: Relation[A, B] | Relation[B, A],
        toMeta: ModelMeta[B]
    ): Argument =
      fr" inner join" ++ toMeta.table.name ++ fr" on" ++ relation.joinCondition

  object GeneralOperators:
    val leftParen: Argument  = fr"("
    val rightParen: Argument = fr")"
    val comma: Argument      = fr","

  object ConditionOperators:
    val and: ConditionOperator = fr"and"
    val or: ConditionOperator  = fr"or"

  object Commands:
    val update: Command = fr"update"
    val insert: Command = fr"insert into"
    val delete: Command = fr"delete from"
    val select: Command = fr"select * from"

  object Arguments:
    val where: Argument  = fr" where"
    val values: Argument = fr" values"
    val set: Argument    = fr" set"

  case class Table(name: Fragment)

  trait Completable(query: Query):
    def complete: Argument =
      query.arguments.foldFragments

    def construct: Fragment =
      (List(query.command, query.table.name) ++ query.arguments).foldFragments

  val * : "*" = "*"
