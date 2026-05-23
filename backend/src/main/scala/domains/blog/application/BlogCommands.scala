package domains.blog.application



import cats.effect.IO
import domains.blog.table.blog.BlogTable
import domains.user.model.Username

import java.sql.Connection

object BlogCommands:
  export BlogCommandResults.*
  export BlogMutationCommands.*
  export BlogQueryCommands.*

  def authorContribution(connection: Connection, authorUsername: Username): IO[BigDecimal] =
    BlogTable.contributionByAuthor(connection, authorUsername)
