/**
 * 英文资源访问控制消息，覆盖公开、授权用户/用户组和管理权限编辑文案。
 */
export const enResourceAccessMessages: Record<string, string> = {
  'resourceAccess.public': 'Public access',
  'resourceAccess.publicDescription': 'Turn this on to make the resource visible to all signed-in users.',
  'resourceAccess.groups': 'Granted user groups',
  'resourceAccess.groupsHint': 'Use commas or new lines. Group slugs must already exist.',
  'resourceAccess.users': 'Granted users',
  'resourceAccess.usersHint': 'Use commas or new lines. Usernames must already exist.',
  'resourceAccess.managerGroups': 'Problem manager groups',
  'resourceAccess.managerGroupsHint': 'Members of these groups can manage the resource.',
  'resourceAccess.managerUsers': 'Problem managers',
  'resourceAccess.managerUsersHint': 'These users can edit, delete, and manage data for the resource.',
  'resourceAccess.badge.public': 'Public',
  'resourceAccess.badge.shared': 'Shared',
  'resourceAccess.badge.restricted': 'Restricted',
  'resourceAccess.summary.visibleAll': 'Visible to all signed-in users.',
  'resourceAccess.summary.visibleGrantedOnly': 'Visible only to explicitly granted viewers and global managers.',
  'resourceAccess.summary.sharedWith': 'Shared with {{summary}}.',
  'resourceAccess.summary.managedGlobalOnly': 'Managed only by global managers.',
  'resourceAccess.summary.managedBy': 'Managed by {{summary}} and global managers.',
  'resourceAccess.summary.group.one': '{{count}} group',
  'resourceAccess.summary.group.other': '{{count}} groups',
  'resourceAccess.summary.user.one': '{{count}} user',
  'resourceAccess.summary.user.other': '{{count}} users',
  'resourceAccess.summary.join': '{{left}} and {{right}}',
}
