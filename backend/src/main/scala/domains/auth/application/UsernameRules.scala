package domains.auth.application



import domains.user.model.Username

object UsernameRules:

  def validate(username: Username): Option[String] =
    Username.parse(username.value).left.toOption
