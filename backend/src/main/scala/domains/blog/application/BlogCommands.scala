package domains.blog.application



import cats.effect.IO
import domains.blog.table.blog.BlogPostQueryTable
import domains.user.objects.Username

import java.sql.Connection

object BlogCommands:
  export BlogCommandResults.*
  export BlogMutationCommands.*
  export BlogQueryCommands.*

  def authorContribution(connection: Connection, authorUsername: Username): IO[BigDecimal] =
    BlogPostQueryTable.contributionByAuthor(connection, authorUsername)
