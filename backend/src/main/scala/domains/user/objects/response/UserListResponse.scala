package domains.user.objects.response


import shared.objects.PageResponse

type UserListResponse = PageResponse[AuthUserListItem]
