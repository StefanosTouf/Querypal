package logic

import logic.FragmentOperations.Argument._
import logic.FragmentOperations._
import doobie.util.fragment.Fragment
import doobie.util.update.Update0

/** The query in its preconstructed form
  */
case class Query(
    command: Command,
    table: Table,
    arguments: List[Argument] = List(),
    conditions: ConditionList = ConditionList(),
    joins: List[Argument] = List()
)

object Query:
  extension (query: Query)
    def complete: Argument =
      query.conditions.conditions.flatten.foldArgs

    def construct: Fragment =
      Update0(
        (List(query.command, query.table.name)
          ++ query.joins
          ++ query.arguments
          :+ query.conditions.fold).foldArgs.toString,
        None
      ).toFragment