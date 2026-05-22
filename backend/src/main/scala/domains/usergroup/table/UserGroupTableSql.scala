package domains.usergroup.table



object UserGroupTableSql:

  val countVisibleSql: String =
    """
      |select count(*) as total_items
      |from user_groups ug
      |where
      |  ? = true
      |  or lower(ug.owner_username) = lower(?)
      |  or exists (
      |    select 1
      |    from user_group_memberships ugm
      |    where ugm.user_group_id = ug.id and lower(ugm.username) = lower(?)
      |  )
      |""".stripMargin

  val listVisibleSql: String =
    """
      |select ug.id, ug.slug, ug.name, ug.description, ug.owner_username, ug.created_at, ug.updated_at
      |from user_groups ug
      |where
      |  ? = true
      |  or lower(ug.owner_username) = lower(?)
      |  or exists (
      |    select 1
      |    from user_group_memberships ugm
      |    where ugm.user_group_id = ug.id and lower(ugm.username) = lower(?)
      |  )
      |order by ug.updated_at desc, ug.slug asc
      |limit ? offset ?
      |""".stripMargin

  val findBySlugSql: String =
    """
      |select id, slug, name, description, owner_username, created_at, updated_at
      |from user_groups
      |where slug = ?
      |""".stripMargin

  val insertSql: String =
    """
      |insert into user_groups (id, slug, name, description, visibility, owner_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, name, description, owner_username, created_at, updated_at
      |""".stripMargin

  val insertOwnerMembershipSql: String =
    """
      |insert into user_group_memberships (user_group_id, username, role, joined_at)
      |values (?, ?, 'owner', ?)
      |""".stripMargin

  val listMembersSql: String =
    """
      |select ugm.username, au.display_name, ugm.role, ugm.joined_at
      |from user_group_memberships ugm
      |join auth_users au on au.username = ugm.username
      |where ugm.user_group_id = ?
      |order by
      |  case ugm.role
      |    when 'owner' then 1
      |    when 'manager' then 2
      |    else 3
      |  end asc,
      |  lower(ugm.username) asc
      |""".stripMargin

  val updateSql: String =
    """
      |update user_groups
      |set name = ?, description = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  val deleteSql: String =
    """
      |delete from user_groups
      |where id = ?
      |""".stripMargin

  val userExistsSql: String =
    """
      |select 1
      |from auth_users
      |where username = ?
      |""".stripMargin

  val membershipExistsSql: String =
    """
      |select 1
      |from user_group_memberships
      |where user_group_id = ? and username = ?
      |""".stripMargin

  val listGroupSlugsForMemberSql: String =
    """
      |select ug.slug
      |from user_group_memberships ugm
      |join user_groups ug on ug.id = ugm.user_group_id
      |where ugm.username = ?
      |order by ug.slug asc
      |""".stripMargin

  val addMemberSql: String =
    """
      |insert into user_group_memberships (user_group_id, username, role, joined_at)
      |values (?, ?, ?, ?)
      |""".stripMargin

  val updateMemberRoleSql: String =
    """
      |update user_group_memberships
      |set role = ?
      |where user_group_id = ? and username = ?
      |""".stripMargin

  val deleteMemberSql: String =
    """
      |delete from user_group_memberships
      |where user_group_id = ? and username = ?
      |""".stripMargin

  val updateOwnerUsernameSql: String =
    """
      |update user_groups
      |set owner_username = ?, updated_at = ?
      |where id = ?
      |""".stripMargin
