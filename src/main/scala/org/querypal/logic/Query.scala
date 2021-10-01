package org.querypal.logic

import FragmentOperations.Argument._
import FragmentOperations._
import doobie.util.fragment.Fragment
import doobie.util.update.Update0

/** The query in its preconstructed form
  */
case class Query(
    command: Command,
    table: Table,
    arguments: List[Argument] = List(),
    conditionList: ConditionList = ConditionList(),
    joins: List[Argument] = List()
)

object Query:
  extension (query: Query)
    def complete: Argument = query.conditionList.conditions.flatten.foldArgs

    def construct: Fragment = Update0(
      query.constructString,
      None
    ).toFragment

    def constructString: String = (List(query.command, query.table.name)
      ++ query.joins
      ++ query.arguments
      :+ query.conditionList.fold).foldArgs.toString.trim
      .replaceAll(" +", " ")