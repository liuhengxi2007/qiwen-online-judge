package api

import cats.effect.IO
import objects.{DemoNote, SaveDemoNoteRequest}
import tables.NoteTable
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.Connection

object SaveDemoNotePlanner extends ConnectionApiPlan[SaveDemoNoteRequest, DemoNote]:

  private val logger = Slf4jLogger.getLogger[IO]

  override val name: String = "SaveDemoNotePlanner"

  override def plan(request: SaveDemoNoteRequest, connection: Connection): IO[DemoNote] =
    for
      _ <- logger.info(s"SaveDemoNotePlanner started, title=${request.title.value}")
      note <- NoteTable.insert(connection, request)
      _ <- logger.info(s"SaveDemoNotePlanner finished, noteId=${note.id.value}")
    yield note
