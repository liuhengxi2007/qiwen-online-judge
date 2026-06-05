package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}

enum JudgeFailureReason:
  case JudgeTaskBuildFailed
  case JudgerRuntimeFailed
  case CheckerCompileFailed
  case CheckerRuntimeFailed
  case InteractorCompileFailed
  case InteractorRuntimeFailed
  case ProblemDataLoadFailed
  case SystemError

object JudgeFailureReason:
  def render(value: JudgeFailureReason): String =
    value match
      case JudgeFailureReason.JudgeTaskBuildFailed => "judge_task_build_failed"
      case JudgeFailureReason.JudgerRuntimeFailed => "judger_runtime_failed"
      case JudgeFailureReason.CheckerCompileFailed => "checker_compile_failed"
      case JudgeFailureReason.CheckerRuntimeFailed => "checker_runtime_failed"
      case JudgeFailureReason.InteractorCompileFailed => "interactor_compile_failed"
      case JudgeFailureReason.InteractorRuntimeFailed => "interactor_runtime_failed"
      case JudgeFailureReason.ProblemDataLoadFailed => "problem_data_load_failed"
      case JudgeFailureReason.SystemError => "system_error"

  given Encoder[JudgeFailureReason] = Encoder.encodeString.contramap(render)
  given Decoder[JudgeFailureReason] = Decoder.decodeString.emap {
    case "judge_task_build_failed" => Right(JudgeFailureReason.JudgeTaskBuildFailed)
    case "judger_runtime_failed" => Right(JudgeFailureReason.JudgerRuntimeFailed)
    case "checker_compile_failed" => Right(JudgeFailureReason.CheckerCompileFailed)
    case "checker_runtime_failed" => Right(JudgeFailureReason.CheckerRuntimeFailed)
    case "interactor_compile_failed" => Right(JudgeFailureReason.InteractorCompileFailed)
    case "interactor_runtime_failed" => Right(JudgeFailureReason.InteractorRuntimeFailed)
    case "problem_data_load_failed" => Right(JudgeFailureReason.ProblemDataLoadFailed)
    case "system_error" => Right(JudgeFailureReason.SystemError)
    case other => Left(s"Unsupported judge failure reason: $other")
  }
