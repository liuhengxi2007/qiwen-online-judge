# Resource Lifecycle Matrix

This document tracks which user-facing create or add flows currently have a matching delete, remove, or reversal flow.

The goal is not to force every action into a symmetric CRUD shape.

The goal is to make lifecycle coverage explicit, so product and engineering decisions around protected resources, ownership, and cleanup are visible instead of accidental.

## Rule Of Thumb

Prefer a matching removal or reversal flow when:

- the action creates a durable resource
- the action creates a durable relationship
- users reasonably expect cleanup or undo capability

Do not force symmetry when:

- the reverse action would violate ownership or safety rules
- the action is better modeled as an update instead of a deletion
- resource cleanup must be gated by policy checks

## Matrix

| Create or Add Flow | Matching Delete or Remove Flow | Status | Notes |
| --- | --- | --- | --- |
| Register user | Delete user | Partial | Site managers can delete users. The system blocks self-deletion, admin deletion, and deletion of users who still own resources. |
| Create problem | Delete problem | Covered | Problems can be deleted by authorized actors. |
| Upload problem data file | Delete data file or clear data | Covered | Problem managers can remove individual files or clear the problem data set. Uploaded zip files expand into data files. |
| Create problem set | Delete problem set | Covered | Problem sets can be deleted by authorized actors. |
| Create user group | Delete user group | Covered | User groups can be deleted by the owner or a site manager. |
| Add problem to problem set | Remove problem from problem set | Covered | The relation is explicitly reversible. |
| Add member to user group | Remove member from user group | Partial | Members can be removed, but the current owner cannot be removed directly. Ownership must be transferred first. |
| Transfer user group ownership | Transfer ownership again | Covered | Ownership transfer is modeled as an explicit governance action rather than member removal. |
| Create submission | Delete submission | Not supported | Submissions are durable judge records. They are corrected by status/result updates rather than user deletion. |
| Claim judge task | Report judge result | Covered | Judgers claim tasks through the backend judge API and complete the lifecycle by reporting results. |
| Create blog | Delete blog | Covered | Blog authors can edit or delete their own blogs. Visibility and general/problem type are updates. |
| Link blog to problem | Change blog type or linked problem | Covered | A problem blog remains visible in global and user blog lists while also appearing in the problem blog area. |
| Add blog comment or reply | Delete comment | Covered | Comment authors can edit or delete their own comments and replies. |
| Like or dislike blog/comment | Click same vote again to clear | Covered | Votes are toggleable; switching between up/down replaces the prior vote. |
| Grant site manager or problem manager permission | Clear permission flag | Covered | This is an update flow, not a delete flow. The reverse action is unchecking the permission. |
| Update user settings | Update user settings again | Covered | Settings are overwritten, not deleted as standalone resources. |
| Login and create session | Logout or session revocation | Covered | Sessions are revoked rather than deleted through a resource page. |

## Protected Cases

Some flows intentionally do not support unrestricted deletion.

### User Deletion

User deletion is intentionally protected.

The backend currently blocks deletion when:

- the actor is not a site manager
- the actor attempts to delete the seeded `admin` account
- the actor attempts to delete their own account
- the target user still owns one or more problems, problem sets, or user groups

This means user deletion is available, but it is not a universal inverse of registration.

### User Group Ownership

The current owner of a user group cannot be removed as a normal member.

The expected workflow is:

1. transfer ownership to another member
2. then remove the former owner if needed

This is an intentional policy constraint, not a missing route.

## Current Gaps

The main remaining asymmetries are product-policy asymmetries, not missing basic endpoints.

- User registration is reversible only through site-manager deletion, not user self-service deletion.
- User deletion requires prior ownership cleanup instead of cascading through owned resources.
- User group owners require ownership transfer before they can be removed from the group.
- Submissions currently do not have a user-facing deletion flow because they are audit-style judge records.

## Non-Goals

This matrix does not imply that:

- every update needs a separate delete action
- every protected resource should become cascade-deletable
- ownership constraints should be removed for the sake of symmetry alone
