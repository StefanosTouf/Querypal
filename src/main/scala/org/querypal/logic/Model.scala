package org.querypal.logic

import Field._
import scala.deriving.Mirror
import DeriveModelMeta._
import FragmentOperations.Table

object Model:

  trait ToTypeDescription[A] {
    def toTypeDescription(name: String): String
  }

  given ToTypeDescription[String] with {
    def toTypeDescription(name: String): String = s"$name varchar not null"
  }

  given ToTypeDescription[Int] with {
    def toTypeDescription(name: String): String = s"$name integer not null"
  }

  /** The trait to be extended by the model of A. It provides helpful type
    * inference in other parts of the pipeline and some helpful methods to
    * construct the model
    */
  trait Model[A]:
    protected inline def deriveMeta(tableName: String, fields: Column[?, ?]*)(
        using m: Mirror.ProductOf[A]
    ) = deriveModelMeta[m.MirroredMonoType](tableName)(fields)

    protected def column[B: ToTypeDescription](name: String): Column[A, B] =
      Column(name)

  /** A type class applied to A, containing metadata about the model of A.
    */
  trait ModelMeta[A]:
    val primaryKeyName: String
    val table: Table
    val typeDescriptions: Seq[String]

    /** Deconstructs an instance of A so it can be used in insert queries
      */
    def map(a: A): (Seq[String], Seq[String])
