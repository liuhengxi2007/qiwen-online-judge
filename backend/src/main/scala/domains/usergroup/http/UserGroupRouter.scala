package domains.usergroup.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.usergroup.http.api.ListUserGroups
import domains.usergroup.http.api.GetUserGroup
import domains.usergroup.http.api.CreateUserGroup
import domains.usergroup.http.api.UpdateUserGroup
import domains.usergroup.http.api.DeleteUserGroup
import domains.usergroup.http.api.AddUserGroupMember
import domains.usergroup.http.api.UpdateUserGroupMemberRole
import domains.usergroup.http.api.RemoveUserGroupMember
import domains.auth.application.SessionStore
import org.http4s.HttpRoutes

object UserGroupRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    ListUserGroups.routes(databaseSession, sessionStore) <+>
      GetUserGroup.routes(databaseSession, sessionStore) <+>
      CreateUserGroup.routes(databaseSession, sessionStore) <+>
      UpdateUserGroup.routes(databaseSession, sessionStore) <+>
      DeleteUserGroup.routes(databaseSession, sessionStore) <+>
      AddUserGroupMember.routes(databaseSession, sessionStore) <+>
      UpdateUserGroupMemberRole.routes(databaseSession, sessionStore) <+>
      RemoveUserGroupMember.routes(databaseSession, sessionStore)
