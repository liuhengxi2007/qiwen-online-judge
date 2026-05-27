package domains.auth.application



import domains.user.objects.Username

object UsernameRules:

  def validate(username: Username): Option[String] =
    Username.parse(username.value).left.toOption
