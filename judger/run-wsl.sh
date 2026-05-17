#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${JUDGE_TOKEN:-}" ]]; then
  export JUDGE_TOKEN="dev-judge-token"
fi

if [[ -z "${JUDGER_ID_PREFIX:-}" ]]; then
  export JUDGER_ID_PREFIX="local-judger"
fi

if [[ -z "${POLL_INTERVAL_MS:-}" ]]; then
  export POLL_INTERVAL_MS="2000"
fi

if [[ -z "${CXX:-}" ]]; then
  export CXX="g++"
fi

if [[ -z "${PYTHON3:-}" ]]; then
  export PYTHON3="python3"
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Java is required inside WSL." >&2
  echo "Install it in WSL, for example: sudo apt update && sudo apt install -y openjdk-21-jdk" >&2
  exit 1
fi

if ! command -v sbt >/dev/null 2>&1; then
  echo "sbt is required inside WSL." >&2
  echo "Install a Linux sbt inside WSL instead of relying on the Windows sbt path." >&2
  exit 1
fi

SBT_BIN="$(command -v sbt)"
case "${SBT_BIN}" in
  /mnt/c/*)
    echo "The sbt found in WSL is the Windows executable: ${SBT_BIN}" >&2
    echo "Install a native Linux sbt inside WSL and ensure it appears before Windows paths in PATH." >&2
    exit 1
    ;;
esac

if ! command -v "${CXX}" >/dev/null 2>&1; then
  echo "C++ compiler '${CXX}' was not found inside WSL." >&2
  exit 1
fi

export CXX="$(command -v "${CXX}")"

if ! command -v "${PYTHON3}" >/dev/null 2>&1; then
  echo "Python 3 interpreter '${PYTHON3}' was not found inside WSL." >&2
  exit 1
fi

export PYTHON3="$(command -v "${PYTHON3}")"

if ! command -v "${ISOLATE_BIN:-isolate}" >/dev/null 2>&1; then
  echo "isolate is required for safe judging inside WSL." >&2
  exit 1
fi

probe_backend_url() {
  local candidate="$1"
  if [[ -z "${candidate}" ]]; then
    return 1
  fi

  if ! command -v curl >/dev/null 2>&1; then
    return 1
  fi

  curl --silent --show-error --fail --max-time 2 "${candidate}/api/health" >/dev/null 2>&1
}

resolve_backend_base_url() {
  local candidates=()

  if [[ -n "${BACKEND_BASE_URL:-}" ]]; then
    echo "${BACKEND_BASE_URL}"
    return 0
  fi

  candidates+=("http://localhost:8080")

  if command -v ip >/dev/null 2>&1; then
    local gateway_ip
    gateway_ip="$(ip route show default 2>/dev/null | awk '/default/ {print $3; exit}')"
    if [[ -n "${gateway_ip}" ]]; then
      candidates+=("http://${gateway_ip}:8080")
    fi
  fi

  if [[ -f /etc/resolv.conf ]]; then
    local resolv_ip
    resolv_ip="$(awk '/nameserver/ {print $2; exit}' /etc/resolv.conf)"
    if [[ -n "${resolv_ip}" ]]; then
      candidates+=("http://${resolv_ip}:8080")
    fi
  fi

  local seen=""
  local candidate=""
  for candidate in "${candidates[@]}"; do
    if [[ -z "${candidate}" ]]; then
      continue
    fi
    if [[ " ${seen} " == *" ${candidate} "* ]]; then
      continue
    fi
    seen="${seen} ${candidate}"

    if probe_backend_url "${candidate}"; then
      echo "${candidate}"
      return 0
    fi
  done

  return 1
}

wait_for_backend_base_url() {
  local attempts=0
  local resolved=""
  while true; do
    if resolved="$(resolve_backend_base_url)"; then
      echo "${resolved}"
      return 0
    fi

    attempts=$((attempts + 1))
    if (( attempts == 1 || attempts % 10 == 0 )); then
      echo "Waiting for backend to become reachable at port 8080..." >&2
    fi
    sleep 1
  done
}

echo "Compiling judger..."
sbt compile

resolved_backend_base_url="$(wait_for_backend_base_url)"
export BACKEND_BASE_URL="${resolved_backend_base_url}"

echo "Starting WSL judger prefix ${JUDGER_ID_PREFIX} against ${BACKEND_BASE_URL}"

sbt run
