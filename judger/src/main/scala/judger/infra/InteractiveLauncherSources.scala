package judger.infra

/** 交互题辅助 launcher 的内嵌 C/C++ 源码，编译后用于忽略 SIGPIPE、FIFO 重定向和策略读监控。 */
object InteractiveLauncherSources:
  private[infra] val SigpipeIgnore: String =
    """#include <signal.h>
      |#include <stdio.h>
      |#include <stddef.h>
      |#include <stdlib.h>
      |#include <string.h>
      |#include <unistd.h>
      |
      |static int prepend_ld_preload(const char* library_path) {
      |  const char* existing = getenv("LD_PRELOAD");
      |  if (existing == NULL || existing[0] == '\0') {
      |    return setenv("LD_PRELOAD", library_path, 1);
      |  }
      |
      |  size_t library_len = strlen(library_path);
      |  size_t existing_len = strlen(existing);
      |  char* combined = (char*)malloc(library_len + 1 + existing_len + 1);
      |  if (combined == NULL) {
      |    return -1;
      |  }
      |
      |  memcpy(combined, library_path, library_len);
      |  combined[library_len] = ':';
      |  memcpy(combined + library_len + 1, existing, existing_len + 1);
      |  int result = setenv("LD_PRELOAD", combined, 1);
      |  free(combined);
      |  return result;
      |}
      |
      |int main(int argc, char** argv) {
      |  if (argc < 2) {
      |    return 127;
      |  }
      |
      |  int command_index = 1;
      |  if (strcmp(argv[1], "--strategy-provider-read-monitor") == 0) {
      |    if (argc < 6) {
      |      return 127;
      |    }
      |    if (prepend_ld_preload(argv[2]) != 0 ||
      |        setenv("QIWEN_STRATEGY_PROVIDER_READ_FIFO", argv[3], 1) != 0 ||
      |        setenv("QIWEN_STRATEGY_PROVIDER_READ_LOG", argv[4], 1) != 0) {
      |      return 127;
      |    }
      |    command_index = 5;
      |  }
      |
      |  if (signal(SIGPIPE, SIG_IGN) == SIG_ERR) {
      |    return 127;
      |  }
      |  execvp(argv[command_index], argv + command_index);
      |  perror("execvp");
      |  return 127;
      |}
      |""".stripMargin

  private[infra] val StrategyProviderReadMonitor: String =
    """#define _GNU_SOURCE
      |#include <dlfcn.h>
      |#include <errno.h>
      |#include <fcntl.h>
      |#include <limits.h>
      |#include <pthread.h>
      |#include <stdarg.h>
      |#include <stddef.h>
      |#include <stdint.h>
      |#include <stdio.h>
      |#include <stdlib.h>
      |#include <string.h>
      |#include <sys/types.h>
      |#include <sys/uio.h>
      |#include <time.h>
      |#include <unistd.h>
      |
      |typedef int (*open_fn_t)(const char*, int, ...);
      |typedef int (*openat_fn_t)(int, const char*, int, ...);
      |typedef ssize_t (*read_fn_t)(int, void*, size_t);
      |typedef ssize_t (*readv_fn_t)(int, const struct iovec*, int);
      |typedef int (*close_fn_t)(int);
      |typedef FILE* (*fopen_fn_t)(const char*, const char*);
      |typedef int (*fclose_fn_t)(FILE*);
      |typedef size_t (*fread_fn_t)(void*, size_t, size_t, FILE*);
      |typedef char* (*fgets_fn_t)(char*, int, FILE*);
      |typedef int (*fgetc_fn_t)(FILE*);
      |typedef ssize_t (*getline_fn_t)(char**, size_t*, FILE*);
      |typedef ssize_t (*getdelim_fn_t)(char**, size_t*, int, FILE*);
      |typedef int (*vfscanf_fn_t)(FILE*, const char*, va_list);
      |
      |static pthread_once_t symbols_once = PTHREAD_ONCE_INIT;
      |static pthread_once_t config_once = PTHREAD_ONCE_INIT;
      |static pthread_mutex_t state_mutex = PTHREAD_MUTEX_INITIALIZER;
      |static pthread_mutex_t log_mutex = PTHREAD_MUTEX_INITIALIZER;
      |
      |static open_fn_t real_open_fn = NULL;
      |static open_fn_t real_open64_fn = NULL;
      |static openat_fn_t real_openat_fn = NULL;
      |static openat_fn_t real_openat64_fn = NULL;
      |static read_fn_t real_read_fn = NULL;
      |static read_fn_t real___read_fn = NULL;
      |static read_fn_t real___libc_read_fn = NULL;
      |static readv_fn_t real_readv_fn = NULL;
      |static close_fn_t real_close_fn = NULL;
      |static fopen_fn_t real_fopen_fn = NULL;
      |static fopen_fn_t real_fopen64_fn = NULL;
      |static fclose_fn_t real_fclose_fn = NULL;
      |static fread_fn_t real_fread_fn = NULL;
      |static fread_fn_t real_fread_unlocked_fn = NULL;
      |static fgets_fn_t real_fgets_fn = NULL;
      |static fgetc_fn_t real_fgetc_fn = NULL;
      |static getline_fn_t real_getline_fn = NULL;
      |static getdelim_fn_t real_getdelim_fn = NULL;
      |static vfscanf_fn_t real_vfscanf_fn = NULL;
      |static vfscanf_fn_t real___isoc99_vfscanf_fn = NULL;
      |
      |static char tracked_fds[4096];
      |static FILE* tracked_files[1024];
      |static char target_raw[PATH_MAX];
      |static char target_real[PATH_MAX];
      |static char log_path[PATH_MAX];
      |static int monitor_enabled = 0;
      |static unsigned long long next_seq = 1;
      |static __thread int in_monitor = 0;
      |
      |static void load_symbols(void) {
      |  real_open_fn = (open_fn_t)dlsym(RTLD_NEXT, "open");
      |  real_open64_fn = (open_fn_t)dlsym(RTLD_NEXT, "open64");
      |  real_openat_fn = (openat_fn_t)dlsym(RTLD_NEXT, "openat");
      |  real_openat64_fn = (openat_fn_t)dlsym(RTLD_NEXT, "openat64");
      |  real_read_fn = (read_fn_t)dlsym(RTLD_NEXT, "read");
      |  real___read_fn = (read_fn_t)dlsym(RTLD_NEXT, "__read");
      |  real___libc_read_fn = (read_fn_t)dlsym(RTLD_NEXT, "__libc_read");
      |  real_readv_fn = (readv_fn_t)dlsym(RTLD_NEXT, "readv");
      |  real_close_fn = (close_fn_t)dlsym(RTLD_NEXT, "close");
      |  real_fopen_fn = (fopen_fn_t)dlsym(RTLD_NEXT, "fopen");
      |  real_fopen64_fn = (fopen_fn_t)dlsym(RTLD_NEXT, "fopen64");
      |  real_fclose_fn = (fclose_fn_t)dlsym(RTLD_NEXT, "fclose");
      |  real_fread_fn = (fread_fn_t)dlsym(RTLD_NEXT, "fread");
      |  real_fread_unlocked_fn = (fread_fn_t)dlsym(RTLD_NEXT, "fread_unlocked");
      |  real_fgets_fn = (fgets_fn_t)dlsym(RTLD_NEXT, "fgets");
      |  real_fgetc_fn = (fgetc_fn_t)dlsym(RTLD_NEXT, "fgetc");
      |  real_getline_fn = (getline_fn_t)dlsym(RTLD_NEXT, "getline");
      |  real_getdelim_fn = (getdelim_fn_t)dlsym(RTLD_NEXT, "getdelim");
      |  real_vfscanf_fn = (vfscanf_fn_t)dlsym(RTLD_NEXT, "vfscanf");
      |  real___isoc99_vfscanf_fn = (vfscanf_fn_t)dlsym(RTLD_NEXT, "__isoc99_vfscanf");
      |
      |  if (real_open64_fn == NULL) real_open64_fn = real_open_fn;
      |  if (real_openat64_fn == NULL) real_openat64_fn = real_openat_fn;
      |  if (real___read_fn == NULL) real___read_fn = real_read_fn;
      |  if (real___libc_read_fn == NULL) real___libc_read_fn = real_read_fn;
      |  if (real_fopen64_fn == NULL) real_fopen64_fn = real_fopen_fn;
      |  if (real_fread_unlocked_fn == NULL) real_fread_unlocked_fn = real_fread_fn;
      |  if (real___isoc99_vfscanf_fn == NULL) real___isoc99_vfscanf_fn = real_vfscanf_fn;
      |}
      |
      |static void load_config(void) {
      |  const char* target = getenv("QIWEN_STRATEGY_PROVIDER_READ_FIFO");
      |  const char* log = getenv("QIWEN_STRATEGY_PROVIDER_READ_LOG");
      |  if (target == NULL || target[0] == '\0' || log == NULL || log[0] == '\0') {
      |    return;
      |  }
      |
      |  snprintf(target_raw, sizeof(target_raw), "%s", target);
      |  snprintf(log_path, sizeof(log_path), "%s", log);
      |  int previous_in_monitor = in_monitor;
      |  in_monitor = 1;
      |  if (realpath(target, target_real) == NULL) {
      |    target_real[0] = '\0';
      |  }
      |  in_monitor = previous_in_monitor;
      |  monitor_enabled = 1;
      |}
      |
      |static int needs_open_mode(int flags) {
      |#ifdef O_TMPFILE
      |  return (flags & O_CREAT) != 0 || ((flags & O_TMPFILE) == O_TMPFILE);
      |#else
      |  return (flags & O_CREAT) != 0;
      |#endif
      |}
      |
      |static int resolve_path(int dirfd, const char* path, char* out, size_t out_size) {
      |  if (path == NULL || path[0] == '\0') {
      |    return 0;
      |  }
      |
      |  char candidate[PATH_MAX];
      |  if (path[0] == '/') {
      |    snprintf(candidate, sizeof(candidate), "%s", path);
      |  } else {
      |    char base[PATH_MAX];
      |    if (dirfd == AT_FDCWD) {
      |      if (getcwd(base, sizeof(base)) == NULL) {
      |        return 0;
      |    }
      |    } else {
      |      char proc_path[64];
      |      snprintf(proc_path, sizeof(proc_path), "/proc/self/fd/%d", dirfd);
      |      ssize_t length = readlink(proc_path, base, sizeof(base) - 1);
      |      if (length < 0) {
      |        return 0;
      |      }
      |      base[length] = '\0';
      |    }
      |    snprintf(candidate, sizeof(candidate), "%s/%s", base, path);
      |  }
      |
      |  int previous_in_monitor = in_monitor;
      |  in_monitor = 1;
      |  char* resolved = realpath(candidate, out);
      |  in_monitor = previous_in_monitor;
      |  if (resolved != NULL) {
      |    return 1;
      |  }
      |  snprintf(out, out_size, "%s", candidate);
      |  return 1;
      |}
      |
      |static int path_matches_target(int dirfd, const char* path) {
      |  pthread_once(&config_once, load_config);
      |  if (!monitor_enabled || path == NULL) {
      |    return 0;
      |  }
      |  if (strcmp(path, target_raw) == 0) {
      |    return 1;
      |  }
      |
      |  char resolved[PATH_MAX];
      |  if (target_real[0] != '\0' && resolve_path(dirfd, path, resolved, sizeof(resolved))) {
      |    return strcmp(resolved, target_real) == 0;
      |  }
      |  return 0;
      |}
      |
      |static long long monotonic_ms(void) {
      |  struct timespec current;
      |  if (clock_gettime(CLOCK_MONOTONIC, &current) != 0) {
      |    return 0;
      |  }
      |  return (long long)current.tv_sec * 1000LL + current.tv_nsec / 1000000LL;
      |}
      |
      |static void log_line(const char* line) {
      |  pthread_once(&symbols_once, load_symbols);
      |  pthread_once(&config_once, load_config);
      |  if (!monitor_enabled || real_open_fn == NULL || real_close_fn == NULL) {
      |    return;
      |  }
      |
      |  pthread_mutex_lock(&log_mutex);
      |  in_monitor = 1;
      |  int fd = real_open_fn(log_path, O_WRONLY | O_CREAT | O_APPEND | O_CLOEXEC, 0666);
      |  if (fd >= 0) {
      |    size_t length = strlen(line);
      |    const char* current = line;
      |    while (length > 0) {
      |      ssize_t written = write(fd, current, length);
      |      if (written < 0) {
      |        if (errno == EINTR) {
      |          continue;
      |        }
      |        break;
      |      }
      |      current += written;
      |      length -= (size_t)written;
      |    }
      |    real_close_fn(fd);
      |  }
      |  in_monitor = 0;
      |  pthread_mutex_unlock(&log_mutex);
      |}
      |
      |static unsigned long long log_begin(void) {
      |  unsigned long long seq = __sync_fetch_and_add(&next_seq, 1);
      |  char line[160];
      |  snprintf(line, sizeof(line), "begin %llu %lld\n", seq, monotonic_ms());
      |  log_line(line);
      |  return seq;
      |}
      |
      |static void log_end(unsigned long long seq, long long result) {
      |  char line[160];
      |  snprintf(line, sizeof(line), "end %llu %lld %lld\n", seq, monotonic_ms(), result);
      |  log_line(line);
      |}
      |
      |static int is_tracked_fd(int fd) {
      |  int result = 0;
      |  if (fd >= 0 && fd < (int)sizeof(tracked_fds)) {
      |    pthread_mutex_lock(&state_mutex);
      |    result = tracked_fds[fd] != 0;
      |    pthread_mutex_unlock(&state_mutex);
      |  }
      |  return result;
      |}
      |
      |static void track_fd_if_target(int fd, int dirfd, const char* path) {
      |  if (fd < 0 || fd >= (int)sizeof(tracked_fds) || in_monitor || !path_matches_target(dirfd, path)) {
      |    return;
      |  }
      |  pthread_mutex_lock(&state_mutex);
      |  tracked_fds[fd] = 1;
      |  pthread_mutex_unlock(&state_mutex);
      |}
      |
      |static void untrack_fd(int fd) {
      |  if (fd < 0 || fd >= (int)sizeof(tracked_fds)) {
      |    return;
      |  }
      |  pthread_mutex_lock(&state_mutex);
      |  tracked_fds[fd] = 0;
      |  pthread_mutex_unlock(&state_mutex);
      |}
      |
      |static void track_file(FILE* stream) {
      |  if (stream == NULL) {
      |    return;
      |  }
      |  pthread_mutex_lock(&state_mutex);
      |  for (size_t i = 0; i < sizeof(tracked_files) / sizeof(tracked_files[0]); ++i) {
      |    if (tracked_files[i] == NULL || tracked_files[i] == stream) {
      |      tracked_files[i] = stream;
      |      break;
      |    }
      |  }
      |  int fd = fileno(stream);
      |  if (fd >= 0 && fd < (int)sizeof(tracked_fds)) {
      |    tracked_fds[fd] = 1;
      |  }
      |  pthread_mutex_unlock(&state_mutex);
      |}
      |
      |static void untrack_file(FILE* stream) {
      |  if (stream == NULL) {
      |    return;
      |  }
      |  pthread_mutex_lock(&state_mutex);
      |  for (size_t i = 0; i < sizeof(tracked_files) / sizeof(tracked_files[0]); ++i) {
      |    if (tracked_files[i] == stream) {
      |      tracked_files[i] = NULL;
      |    }
      |  }
      |  int fd = fileno(stream);
      |  if (fd >= 0 && fd < (int)sizeof(tracked_fds)) {
      |    tracked_fds[fd] = 0;
      |  }
      |  pthread_mutex_unlock(&state_mutex);
      |}
      |
      |static int is_tracked_file(FILE* stream) {
      |  if (stream == NULL) {
      |    return 0;
      |  }
      |  int fd = fileno(stream);
      |  int result = 0;
      |  pthread_mutex_lock(&state_mutex);
      |  for (size_t i = 0; i < sizeof(tracked_files) / sizeof(tracked_files[0]); ++i) {
      |    if (tracked_files[i] == stream) {
      |      result = 1;
      |      break;
      |    }
      |  }
      |  if (!result && fd >= 0 && fd < (int)sizeof(tracked_fds)) {
      |    result = tracked_fds[fd] != 0;
      |  }
      |  pthread_mutex_unlock(&state_mutex);
      |  return result;
      |}
      |
      |static ssize_t monitored_read(read_fn_t real_fn, int fd, void* buffer, size_t count) {
      |  if (real_fn == NULL) {
      |    errno = ENOSYS;
      |    return -1;
      |  }
      |  if (in_monitor || !is_tracked_fd(fd)) {
      |    return real_fn(fd, buffer, count);
      |  }
      |  unsigned long long seq = log_begin();
      |  ssize_t result = real_fn(fd, buffer, count);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" int open(const char* path, int flags, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  mode_t mode = 0;
      |  if (needs_open_mode(flags)) {
      |    va_list args;
      |    va_start(args, flags);
      |    mode = (mode_t)va_arg(args, int);
      |    va_end(args);
      |  }
      |  int fd = needs_open_mode(flags) ? real_open_fn(path, flags, mode) : real_open_fn(path, flags);
      |  track_fd_if_target(fd, AT_FDCWD, path);
      |  return fd;
      |}
      |
      |extern "C" int open64(const char* path, int flags, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  mode_t mode = 0;
      |  if (needs_open_mode(flags)) {
      |    va_list args;
      |    va_start(args, flags);
      |    mode = (mode_t)va_arg(args, int);
      |    va_end(args);
      |  }
      |  int fd = needs_open_mode(flags) ? real_open64_fn(path, flags, mode) : real_open64_fn(path, flags);
      |  track_fd_if_target(fd, AT_FDCWD, path);
      |  return fd;
      |}
      |
      |extern "C" int openat(int dirfd, const char* path, int flags, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  mode_t mode = 0;
      |  if (needs_open_mode(flags)) {
      |    va_list args;
      |    va_start(args, flags);
      |    mode = (mode_t)va_arg(args, int);
      |    va_end(args);
      |  }
      |  int fd = needs_open_mode(flags) ? real_openat_fn(dirfd, path, flags, mode) : real_openat_fn(dirfd, path, flags);
      |  track_fd_if_target(fd, dirfd, path);
      |  return fd;
      |}
      |
      |extern "C" int openat64(int dirfd, const char* path, int flags, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  mode_t mode = 0;
      |  if (needs_open_mode(flags)) {
      |    va_list args;
      |    va_start(args, flags);
      |    mode = (mode_t)va_arg(args, int);
      |    va_end(args);
      |  }
      |  int fd = needs_open_mode(flags) ? real_openat64_fn(dirfd, path, flags, mode) : real_openat64_fn(dirfd, path, flags);
      |  track_fd_if_target(fd, dirfd, path);
      |  return fd;
      |}
      |
      |extern "C" ssize_t read(int fd, void* buffer, size_t count) {
      |  pthread_once(&symbols_once, load_symbols);
      |  return monitored_read(real_read_fn, fd, buffer, count);
      |}
      |
      |extern "C" ssize_t __read(int fd, void* buffer, size_t count) {
      |  pthread_once(&symbols_once, load_symbols);
      |  return monitored_read(real___read_fn, fd, buffer, count);
      |}
      |
      |extern "C" ssize_t __libc_read(int fd, void* buffer, size_t count) {
      |  pthread_once(&symbols_once, load_symbols);
      |  return monitored_read(real___libc_read_fn, fd, buffer, count);
      |}
      |
      |extern "C" ssize_t readv(int fd, const struct iovec* iov, int iovcnt) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (real_readv_fn == NULL) {
      |    errno = ENOSYS;
      |    return -1;
      |  }
      |  if (in_monitor || !is_tracked_fd(fd)) {
      |    return real_readv_fn(fd, iov, iovcnt);
      |  }
      |  unsigned long long seq = log_begin();
      |  ssize_t result = real_readv_fn(fd, iov, iovcnt);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" int close(int fd) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (!in_monitor) {
      |    untrack_fd(fd);
      |  }
      |  return real_close_fn(fd);
      |}
      |
      |extern "C" FILE* fopen(const char* path, const char* mode) {
      |  pthread_once(&symbols_once, load_symbols);
      |  FILE* stream = real_fopen_fn(path, mode);
      |  if (!in_monitor && stream != NULL && path_matches_target(AT_FDCWD, path)) {
      |    track_file(stream);
      |  }
      |  return stream;
      |}
      |
      |extern "C" FILE* fopen64(const char* path, const char* mode) {
      |  pthread_once(&symbols_once, load_symbols);
      |  FILE* stream = real_fopen64_fn(path, mode);
      |  if (!in_monitor && stream != NULL && path_matches_target(AT_FDCWD, path)) {
      |    track_file(stream);
      |  }
      |  return stream;
      |}
      |
      |extern "C" int fclose(FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (!in_monitor) {
      |    untrack_file(stream);
      |  }
      |  return real_fclose_fn(stream);
      |}
      |
      |extern "C" size_t fread(void* ptr, size_t size, size_t nmemb, FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_fread_fn(ptr, size, nmemb, stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  size_t result = real_fread_fn(ptr, size, nmemb, stream);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" size_t fread_unlocked(void* ptr, size_t size, size_t nmemb, FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_fread_unlocked_fn(ptr, size, nmemb, stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  size_t result = real_fread_unlocked_fn(ptr, size, nmemb, stream);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" char* fgets(char* s, int size, FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_fgets_fn(s, size, stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  char* result = real_fgets_fn(s, size, stream);
      |  log_end(seq, result == NULL ? 0LL : 1LL);
      |  return result;
      |}
      |
      |extern "C" int fgetc(FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_fgetc_fn(stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  int result = real_fgetc_fn(stream);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" ssize_t getline(char** lineptr, size_t* n, FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_getline_fn(lineptr, n, stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  ssize_t result = real_getline_fn(lineptr, n, stream);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" ssize_t getdelim(char** lineptr, size_t* n, int delim, FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_getdelim_fn(lineptr, n, delim, stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  ssize_t result = real_getdelim_fn(lineptr, n, delim, stream);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" int monitored_fscanf(FILE* stream, const char* format, ...) __asm__("fscanf");
      |extern "C" int monitored_isoc99_fscanf(FILE* stream, const char* format, ...) __asm__("__isoc99_fscanf");
      |
      |extern "C" int monitored_fscanf(FILE* stream, const char* format, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  va_list args;
      |  va_start(args, format);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    int result = real_vfscanf_fn(stream, format, args);
      |    va_end(args);
      |    return result;
      |  }
      |  unsigned long long seq = log_begin();
      |  int result = real_vfscanf_fn(stream, format, args);
      |  va_end(args);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" int monitored_isoc99_fscanf(FILE* stream, const char* format, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  va_list args;
      |  va_start(args, format);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    int result = real___isoc99_vfscanf_fn(stream, format, args);
      |    va_end(args);
      |    return result;
      |  }
      |  unsigned long long seq = log_begin();
      |  int result = real___isoc99_vfscanf_fn(stream, format, args);
      |  va_end(args);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |""".stripMargin

  private[infra] val FifoRedirect: String =
    """#include <errno.h>
      |#include <fcntl.h>
      |#include <stdio.h>
      |#include <unistd.h>
      |
      |static int open_stdout_fifo(const char* path) {
      |  for (;;) {
      |    int fd = open(path, O_WRONLY | O_NONBLOCK);
      |    if (fd >= 0) {
      |      return fd;
      |    }
      |    if (errno != ENXIO && errno != EINTR) {
      |      perror("open stdout fifo");
      |      return -1;
      |    }
      |    usleep(1000);
      |  }
      |}
      |
      |static void clear_nonblock(int fd) {
      |  int flags = fcntl(fd, F_GETFL, 0);
      |  if (flags >= 0) {
      |    fcntl(fd, F_SETFL, flags & ~O_NONBLOCK);
      |  }
      |}
      |
      |int main(int argc, char** argv) {
      |  if (argc < 4) {
      |    return 127;
      |  }
      |
      |  int stdin_fd = open(argv[1], O_RDONLY | O_NONBLOCK);
      |  if (stdin_fd < 0) {
      |    perror("open stdin fifo");
      |    return 127;
      |  }
      |
      |  int stdout_fd = open_stdout_fifo(argv[2]);
      |  if (stdout_fd < 0) {
      |    close(stdin_fd);
      |    return 127;
      |  }
      |
      |  clear_nonblock(stdin_fd);
      |  clear_nonblock(stdout_fd);
      |
      |  if (dup2(stdin_fd, STDIN_FILENO) < 0) {
      |    perror("dup2 stdin");
      |    return 127;
      |  }
      |  if (dup2(stdout_fd, STDOUT_FILENO) < 0) {
      |    perror("dup2 stdout");
      |    return 127;
      |  }
      |
      |  if (stdin_fd != STDIN_FILENO) {
      |    close(stdin_fd);
      |  }
      |  if (stdout_fd != STDOUT_FILENO) {
      |    close(stdout_fd);
      |  }
      |
      |  execvp(argv[3], argv + 3);
      |  perror("execvp");
      |  return 127;
      |}
      |""".stripMargin
