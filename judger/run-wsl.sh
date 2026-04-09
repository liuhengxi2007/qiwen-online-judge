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

if resolved_backend_base_url="$(resolve_backend_base_url)"; then
  export BACKEND_BASE_URL="${resolved_backend_base_url}"
else
  echo "Unable to reach the backend from WSL." >&2
  echo "Tried localhost and detected Windows host gateway addresses on port 8080." >&2
  echo "Set BACKEND_BASE_URL manually if your backend is listening elsewhere." >&2
  exit 1
fi

echo "Starting WSL judger prefix ${JUDGER_ID_PREFIX} against ${BACKEND_BASE_URL}"

if ! command -v sbt >/dev/null 2>&1; then
  echo "sbt is required inside WSL." >&2
  exit 1
fi

if ! command -v "${CXX}" >/dev/null 2>&1; then
  echo "C++ compiler '${CXX}' was not found inside WSL." >&2
  exit 1
fi

sbt run
