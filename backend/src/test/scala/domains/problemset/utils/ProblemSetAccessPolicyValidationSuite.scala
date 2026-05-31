package domains.problemset.utils

import munit.CatsEffectSuite
import org.http4s.Status
import shared.api.HttpApiError
import shared.objects.access.{AccessSubject, AccessUsername, BaseAccess, ResourceAccessPolicy}

class ProblemSetAccessPolicyValidationSuite extends CatsEffectSuite:

  test("validateAccessPolicySubjects rejects manager grants") {
    val policy = ResourceAccessPolicy(
      BaseAccess.Restricted,
      viewerGrants = Nil,
      managerGrants = List(AccessSubject.User(AccessUsername("manager-1")))
    )

    ProblemSetAccessPolicyValidation.validateAccessPolicySubjects(null, policy).attempt.map { result =>
      val error = result.left.toOption.collect { case error: HttpApiError => error }

      assertEquals(error.map(_.status), Some(Status.BadRequest))
      assertEquals(error.flatMap(_.fallbackMessage), Some("Problem set access policies do not support manager grants."))
    }
  }

  test("sanitizePolicy omits manager grants") {
    val policy = ResourceAccessPolicy(
      BaseAccess.Restricted,
      viewerGrants = Nil,
      managerGrants = List(AccessSubject.User(AccessUsername("manager-1")))
    )

    assertEquals(ProblemSetAccessPolicyValidation.sanitizePolicy(policy).managerGrants, Nil)
  }
