use std::process::{Child, Command, Stdio};
use std::sync::{Arc, Mutex, atomic::{AtomicBool, Ordering}};
use std::net::TcpStream;
use std::time::{Duration, Instant};
use std::fs;
use std::path::PathBuf;
use tauri::Manager;
use percent_encoding::{utf8_percent_encode, AsciiSet, CONTROLS};

// ── Win32 FFI types & constants (no comctl32 dependency) ────────────

#[allow(non_camel_case_types)]
type HWND = isize;
type WPARAM = usize;
type LPARAM = isize;
type LRESULT = isize;
type HINSTANCE = isize;
type HICON = isize;
type HCURSOR = isize;
type HBRUSH = isize;
type HMENU = isize;
type BOOL = i32;
type UINT = u32;
type DWORD = u32;
type ATOM = u16;
type LPVOID = *mut std::ffi::c_void;
type LPCWSTR = *const u16;

#[repr(C)]
struct POINT { x: i32, y: i32 }

#[repr(C)]
struct MSG {
    hwnd: HWND, message: UINT, _pad1: UINT,
    wparam: WPARAM, lparam: LPARAM,
    time: DWORD, pt: POINT,
    lprivate: DWORD,
}
const _: () = assert!(std::mem::size_of::<MSG>() == 48, "MSG must be 48 bytes");

#[repr(C)]
struct WNDCLASSW {
    style: UINT,
    lpfn_wndproc: Option<unsafe extern "system" fn(HWND, UINT, WPARAM, LPARAM) -> LRESULT>,
    cb_cls_extra: i32, cb_wnd_extra: i32,
    h_instance: HINSTANCE,
    h_icon: HICON, h_cursor: HCURSOR, hbr_background: HBRUSH,
    menu_name: LPCWSTR, class_name: LPCWSTR,
}

#[repr(C)]
struct GUID { data1: u32, data2: u16, data3: u16, data4: [u8; 8] }

#[repr(C)]
struct SYSTEMTIME { w_year: u16, w_month: u16, w_day_of_week: u16, w_day: u16, w_hour: u16, w_minute: u16, w_second: u16, w_milliseconds: u16 }

#[repr(C, align(8))]
struct NOTIFYICONDATAW {
    cb_size: DWORD, _pad0: DWORD,
    hwnd: HWND,
    u_id: UINT, u_flags: UINT,
    u_callback_message: UINT, _pad1: UINT,
    h_icon: HICON,
    sz_tip: [u16; 128],
    dw_state: DWORD, dw_state_mask: DWORD,
    sz_info: [u16; 256],
    u_timeout_or_version: UINT, _pad2: UINT,
    sz_info_title: [u16; 64],
    dw_info_flags: DWORD, _pad3: UINT,
    guid_item: GUID,
    h_balloon_icon: HICON,
}

const CS_HREDRAW: UINT = 2;
const CS_VREDRAW: UINT = 1;
const WS_OVERLAPPEDWINDOW: UINT = 0x00CF0000;
const CW_USEDEFAULT: i32 = 0x80000000u32 as i32;
const IDC_ARROW: LPCWSTR = 32512isize as LPCWSTR;
const WM_USER: UINT = 0x0400;
const WM_LBUTTONUP: UINT = 0x0202;
const WM_RBUTTONUP: UINT = 0x0205;
const WM_COMMAND: UINT = 0x0111;
const WM_DESTROY: UINT = 0x0002;
const WM_QUERYENDSESSION: UINT = 0x0011;
const WM_ENDSESSION: UINT = 0x0016;
const MF_STRING: UINT = 0;
const TPM_RIGHTBUTTON: UINT = 2;
const TPM_BOTTOMALIGN: UINT = 8;
const TPM_RETURNCMD: UINT = 256;
const NIM_ADD: UINT = 0;
const NIM_DELETE: UINT = 2;
const NIF_MESSAGE: UINT = 1;
const NIF_ICON: UINT = 2;
const NIF_TIP: UINT = 4;

extern "system" {
    fn CreateIcon(
        h_instance: HINSTANCE, w: i32, h: i32,
        planes: UINT, bpp: UINT,
        and_bits: *const u8, xor_bits: *const u8,
    ) -> HICON;
    fn CreateWindowExW(
        ex_style: DWORD, class: LPCWSTR, title: LPCWSTR, style: DWORD,
        x: i32, y: i32, w: i32, h: i32,
        parent: HWND, menu: HMENU, instance: HINSTANCE, param: LPVOID,
    ) -> HWND;
    fn RegisterClassW(cls: *const WNDCLASSW) -> ATOM;
    fn DefWindowProcW(h: HWND, m: UINT, w: WPARAM, l: LPARAM) -> LRESULT;
    fn DestroyWindow(h: HWND) -> BOOL;
    fn CreatePopupMenu() -> HMENU;
    fn InsertMenuW(menu: HMENU, pos: UINT, flags: UINT, id: usize, text: LPCWSTR) -> BOOL;
    fn TrackPopupMenu(menu: HMENU, flags: UINT, x: i32, y: i32, reserved: i32, hwnd: HWND, rect: *const std::ffi::c_void) -> BOOL;
    fn DestroyMenu(menu: HMENU) -> BOOL;
    fn GetCursorPos(pt: *mut POINT) -> BOOL;
    fn SetForegroundWindow(h: HWND) -> BOOL;
    fn PostQuitMessage(code: i32);
    fn GetMessageW(msg: *mut MSG, h: HWND, filter_min: UINT, filter_max: UINT) -> BOOL;
    fn TranslateMessage(msg: *const MSG) -> BOOL;
    fn DispatchMessageW(msg: *const MSG) -> LRESULT;
    fn LoadCursorW(inst: HINSTANCE, name: LPCWSTR) -> HCURSOR;
    fn Shell_NotifyIconW(msg: DWORD, data: *mut NOTIFYICONDATAW) -> BOOL;
    fn GetModuleHandleW(name: LPCWSTR) -> HINSTANCE;
    fn DestroyIcon(icon: HICON) -> BOOL;
}

const MAX_RESTART_ATTEMPTS: u32 = 3;
const MONITOR_INTERVAL_SECS: u64 = 5;

/// Save file with native dialog — uses raw GetSaveFileNameW (comdlg32, NOT comctl32).
/// Data is base64-encoded to avoid JSON-serializing huge byte arrays over IPC.
#[tauri::command]
async fn save_file(_app: tauri::AppHandle, data: String, filename: String) -> Result<String, String> {
    use base64::Engine;
    let file_bytes = base64::engine::general_purpose::STANDARD
        .decode(&data)
        .map_err(|e| format!("Base64解码失败: {}", e))?;

    let file_path = tauri::async_runtime::spawn_blocking(move || -> Option<String> {
        let wide_name: Vec<u16> = filename.encode_utf16().chain(std::iter::once(0)).collect();
        let filter: Vec<u16> = "Excel 文件 (*.xlsx)\0*.xlsx\0所有文件 (*.*)\0*.*\0\0\0".encode_utf16().collect();
        // OPENFILENAMEW struct: 152 bytes on x64 (pad to correct size)
        const OFN_SIZE: u32 = 152;
        let mut ofn: [u8; 152] = [0u8; 152];
        // cbSize (u32 at offset 0)
        unsafe { std::ptr::write(ofn.as_mut_ptr() as *mut u32, OFN_SIZE); }
        // hwndOwner (isize at offset 8): NULL = 0
        // lpstrFilter (pointer at offset 24) = LPCWSTR
        unsafe { std::ptr::write(ofn.as_mut_ptr().add(24) as *mut *const u16, filter.as_ptr()); }
        // nFilterIndex (u32 at offset 32) = 1
        unsafe { std::ptr::write(ofn.as_mut_ptr().add(32) as *mut u32, 1); }
        // lpstrFile (pointer at offset 40): writable buffer for returned path
        let mut file_buf: Vec<u16> = wide_name.clone();
        file_buf.resize(512, 0);
        unsafe { std::ptr::write(ofn.as_mut_ptr().add(40) as *mut *mut u16, file_buf.as_mut_ptr()); }
        // nMaxFile (u32 at offset 48) = 512
        unsafe { std::ptr::write(ofn.as_mut_ptr().add(48) as *mut u32, 512); }
        // Flags (u32 at offset 88): OFN_OVERWRITEPROMPT = 2
        unsafe { std::ptr::write(ofn.as_mut_ptr().add(88) as *mut u32, 2); }

        type GetSaveFileNameW = unsafe extern "system" fn(*mut u8) -> i32;
        let result = unsafe {
            let lib = LoadLibraryA(b"comdlg32.dll\0".as_ptr());
            if lib == 0 { return None; }
            let proc = GetProcAddress(lib, b"GetSaveFileNameW\0".as_ptr());
            if proc.is_null() { return None; }
            let func: GetSaveFileNameW = std::mem::transmute(proc);
            func(ofn.as_mut_ptr())
        };
        if result == 0 { return None; }
        String::from_utf16(&file_buf[..file_buf.iter().position(|&c| c == 0).unwrap_or(0)]).ok()
    })
    .await
    .map_err(|e| format!("对话框线程错误: {}", e))?;

    match file_path {
        Some(path) => {
            std::fs::write(&path, &file_bytes).map_err(|e| format!("写入文件失败: {}", e))?;
            Ok(path)
        }
        None => Ok("cancelled".to_string()),
    }
}

/// Shared handle to the Java child process. Replaced by the monitor on restart.
type JavaProcess = Arc<Mutex<Option<Child>>>;

/// Shutdown flag — set by exit handler to prevent monitor from spawning after exit.
struct ShutdownFlag(Arc<AtomicBool>);
struct DataDir(PathBuf);

struct JavaSpawnConfig {
    java_cmd: String,
    jar_str: String,
    install_dir: String,
    data_dir: PathBuf,
    java_log: PathBuf,
    mem_args: Vec<String>,
}

// ── helpers ──────────────────────────────────────────────────────

fn strip_unc_prefix(path: PathBuf) -> PathBuf {
    let s = path.to_string_lossy();
    if s.starts_with(r"\\?\") {
        PathBuf::from(&s[4..])
    } else {
        path
    }
}

fn find_jar_path(_app: &tauri::App) -> PathBuf {
    let path = if cfg!(debug_assertions) {
        let manifest_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
        manifest_dir.join("..").join("target").join("window-system-1.0.0.jar")
    } else {
        let exe_dir = std::env::current_exe()
            .expect("failed to get exe path")
            .parent()
            .expect("failed to get exe dir")
            .to_path_buf();
        exe_dir.join("resources").join("window-system-1.0.0.jar")
    };
    strip_unc_prefix(path)
}

/// 新数据目录为空时，从旧的候选位置（安装目录 或 APPDATA）迁移数据，双向覆盖：
/// ① Program Files 旧安装（数据在安装目录\data，管理员跑过）→ 新 APPDATA；
/// ② 旧 Program Files 安装（数据在 APPDATA）→ 新 D 盘/其他可写路径安装。
/// 仅当目标数据目录为空时执行，避免覆盖现有数据。在 Java 启动前调用，无 H2 锁冲突。
fn migrate_if_empty(data_dir: &PathBuf, exe_dir: &PathBuf) {
    let dst_data = data_dir.join("data");
    let dst_empty = std::fs::read_dir(&dst_data)
        .map(|mut e| e.next().is_none())
        .unwrap_or(true);
    if !dst_empty {
        return;
    }
    let mut sources: Vec<PathBuf> = Vec::new();
    if exe_dir != data_dir {
        sources.push(exe_dir.clone());
    }
    if let Ok(app_data) = std::env::var("APPDATA") {
        let apdata_dir = PathBuf::from(app_data).join("com.shunjumc.window-system");
        if apdata_dir != *data_dir {
            sources.push(apdata_dir);
        }
    }
    for src in sources {
        if src.join("data").join("window_db.mv.db").exists() {
            log::info!("从 {:?} 迁移旧数据到 {:?}", src, data_dir);
            migrate_old_data(&src, data_dir);
            return;
        }
    }
}

/// 旧版本可能把数据留在安装目录/data（管理员运行过）；迁移到新的数据目录。
/// 仅当新目录为空时执行，避免覆盖。在 Java 启动前调用，无 H2 锁冲突。
fn migrate_old_data(old_install_dir: &PathBuf, new_data_dir: &PathBuf) {
    let src = old_install_dir.join("data");
    let dst = new_data_dir.join("data");
    if !src.is_dir() {
        return;
    }
    // 先确保目标 data 目录存在，否则 copy 会静默失败、迁移变空操作
    if std::fs::create_dir_all(&dst).is_err() {
        log::warn!("迁移目标目录创建失败: {:?}", dst);
        return;
    }
    let dst_empty = std::fs::read_dir(&dst)
        .map(|mut e| e.next().is_none())
        .unwrap_or(false);
    if !dst_empty {
        return;
    }
    copy_data_dir_recursive(&src, &dst);
}

/// 递归复制 data 目录（H2 库文件 + images 等子目录——图片存 data/images/，不递归会丢图），
/// 跳过隐藏标记文件（.java_pid 等）
fn copy_data_dir_recursive(src: &std::path::Path, dst: &std::path::Path) {
    let Ok(entries) = std::fs::read_dir(src) else { return; };
    for entry in entries.flatten() {
        let path = entry.path();
        let name = entry.file_name();
        if name.to_string_lossy().starts_with('.') {
            continue; // 跳过 .java_pid 等隐藏标记
        }
        let dest = dst.join(&name);
        if path.is_dir() {
            let _ = std::fs::create_dir_all(&dest);
            copy_data_dir_recursive(&path, &dest);
        } else if path.is_file() {
            if std::fs::copy(&path, &dest).is_ok() {
                log::info!("已迁移旧数据: {:?}", path);
            }
        }
    }
}

/// Build a `data:` URL from inline HTML, so the window can show a loading page
/// immediately while the Java backend is still starting up.
const HTML_ENCODE_SET: &AsciiSet = &CONTROLS
    .add(b' ')
    .add(b'"')
    .add(b'#')
    .add(b'%')
    .add(b'&')
    .add(b'\'')
    .add(b'(')
    .add(b')')
    .add(b'+')
    .add(b',')
    .add(b'/')
    .add(b':')
    .add(b';')
    .add(b'=')
    .add(b'?')
    .add(b'@')
    .add(b'[')
    .add(b']');

fn build_loading_url() -> String {
    let html = r#"<!DOCTYPE html><html><head><meta charset="utf-8"><style>
      html,body{height:100%;margin:0;display:flex;align-items:center;justify-content:center;
      font-family:'Microsoft YaHei','SimSun',sans-serif;background:#f5f6f7;color:#333}
      .box{text-align:center}.title{font-size:22px;font-weight:600;letter-spacing:2px}
      .sub{margin-top:16px;font-size:13px;color:#888}
      .dot{display:inline-block;width:6px;height:6px;border-radius:50%;background:#C5A265;
      margin-left:4px;animation:blink 1.2s infinite}
      .dot:nth-child(2){animation-delay:.2s}.dot:nth-child(3){animation-delay:.4s}
      @keyframes blink{0%,80%,100%{opacity:.2}40%{opacity:1}}
    </style></head><body>
    <div class="box">
      <div class="title">顺居门业管理系统</div>
      <div class="sub">正在启动后台服务<span class="dot"></span><span class="dot"></span><span class="dot"></span></div>
    </div></body></html>"#;
    let encoded = utf8_percent_encode(html, HTML_ENCODE_SET);
    format!("data:text/html;charset=utf-8,{}", encoded)
}

/// Append a diagnostic line to startup.log in the data dir. On a GUI app stdout
/// is lost, so a remote/stripped machine can report what actually happened.
fn diag_log(data_dir: &PathBuf, msg: &str) {
    use std::io::Write;
    if let Ok(mut f) = std::fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(data_dir.join("startup.log"))
    {
        let _ = writeln!(f, "[pid={}] {}", std::process::id(), msg);
    }
}

#[cfg(target_os = "windows")]
fn show_error_dialog(msg: &str) {
    log::error!("{}", msg);
    use std::ffi::OsStr;
    use std::os::windows::ffi::OsStrExt;
    type MessageBoxW = unsafe extern "system" fn(isize, *const u16, *const u16, u32) -> i32;
    let title: Vec<u16> = OsStr::new("顺居门业管理系统\0").encode_wide().collect();
    let body: Vec<u16> = OsStr::new(&format!("{}\0", msg)).encode_wide().collect();
    unsafe {
        let lib = b"user32.dll\0";
        let proc = b"MessageBoxW\0";
        let handle = LoadLibraryA(lib.as_ptr());
        if handle != 0 {
            let func_ptr = GetProcAddress(handle, proc.as_ptr());
            if !func_ptr.is_null() {
                let func: MessageBoxW = std::mem::transmute(func_ptr);
                func(0, body.as_ptr(), title.as_ptr(), 0x10); // MB_ICONERROR
            }
            FreeLibrary(handle);
        }
    }
}

#[cfg(not(target_os = "windows"))]
fn show_error_dialog(msg: &str) {
    log::error!("{}", msg);
    eprintln!("ERROR: {}", msg);
}

extern "system" {
    fn LoadLibraryA(name: *const u8) -> isize;
    fn GetProcAddress(module: isize, name: *const u8) -> *const ();
    fn FreeLibrary(module: isize) -> i32;
    fn CreateJobObjectW(lpJobAttributes: *const core::ffi::c_void, lpName: *const u16) -> isize;
    fn AssignProcessToJobObject(hJob: isize, hProcess: isize) -> i32;
    fn GetCurrentProcess() -> isize;
    fn SetInformationJobObject(hJob: isize, jobObjectInfoClass: i32, lpJobObjectInfo: *const core::ffi::c_void, cbJobObjectInfoLength: u32) -> i32;
    fn SetHandleInformation(hObject: isize, dwMask: u32, dwFlags: u32) -> i32;
    fn GetPhysicallyInstalledSystemMemory(total_kb: *mut u64) -> i32;
    fn GetLocalTime(lp_system_time: *mut SYSTEMTIME);
}

#[cfg(target_os = "windows")]
#[link(name = "shell32")]
#[link(name = "ole32")]
extern "system" {
    fn SHGetKnownFolderPath(rfid: *const GUID, dw_flags: u32, h_token: isize, ppsz_path: *mut *mut u16) -> i32;
    fn CoTaskMemFree(pv: *mut core::ffi::c_void);
}

fn show_error_and_exit(msg: &str) -> ! {
    show_error_dialog(msg);
    std::process::exit(1);
}

/// Set up a Windows Job Object with KILL_ON_JOB_CLOSE.
/// When this process exits (crash, kill, normal exit), Windows kills all child processes.
#[cfg(target_os = "windows")]
fn setup_job_object() {
    unsafe {
        let job = CreateJobObjectW(core::ptr::null(), core::ptr::null());
        if job == 0 {
            log::warn!("CreateJobObjectW failed");
            return;
        }
        // Make job handle non-inheritable so child processes don't keep it alive
        SetHandleInformation(job, 1, 0); // HANDLE_FLAG_INHERIT=1, clear inherit flag
        // Verified by C compiler: sizeof(JOBOBJECT_EXTENDED_LIMIT_INFORMATION) = 144
        // LimitFlags at offset 16, JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x2000
        let mut info = [0u8; 144];
        info[16] = 0x00; // 0x2000 little-endian low byte
        info[17] = 0x20; // 0x2000 little-endian high byte
        let r = SetInformationJobObject(job, 9, info.as_ptr() as *const _, 144);
        if r == 0 {
            log::warn!("SetInformationJobObject failed: {}", std::io::Error::last_os_error());
        }
        let r = AssignProcessToJobObject(job, GetCurrentProcess());
        if r == 0 {
            log::warn!("AssignProcessToJobObject failed: {}", std::io::Error::last_os_error());
        } else {
            log::info!("Job Object configured: KILL_ON_JOB_CLOSE");
        }
    }
}

/// Parse the major version from `java -version` output ("17.0.12" -> 17, legacy "1.8.0" -> 8).
fn java_major_version(java_cmd: &str) -> Option<u32> {
    let out = Command::new(java_cmd).arg("-version").output().ok()?;
    let stdout = String::from_utf8_lossy(&out.stdout);
    let stderr = String::from_utf8_lossy(&out.stderr);
    let text = if stdout.is_empty() { stderr } else { stdout };
    let text = text.to_string();
    let start = text.find("version \"")? + 9;
    let end = start + text[start..].find('"')?;
    let ver = &text[start..end];
    let first = ver.split('.').next()?;
    if first == "1" {
        // legacy scheme: "1.8.0_301" -> Java 8
        ver.split('.').nth(1)?.parse().ok()
    } else {
        first.parse().ok()
    }
}

/// Spring Boot 3.3.7 officially supports Java 17-21. Newer JDKs (25/26+) fail to
/// start the backend, leaving the app stuck on the loading page (black screen).
fn java_version_supported(version: u32) -> bool {
    (17..=21).contains(&version)
}

fn find_java() -> Option<String> {
    let mut candidates = Vec::new();
    if Command::new("java").arg("-version").stdout(Stdio::null()).stderr(Stdio::null()).status().is_ok() {
        candidates.push("java".to_string());
    }
    if let Ok(java_home) = std::env::var("JAVA_HOME") {
        let java_bin = PathBuf::from(&java_home).join("bin").join("java.exe");
        if java_bin.exists() {
            log::info!("Found Java via JAVA_HOME: {:?}", java_bin);
            candidates.push(java_bin.to_string_lossy().to_string());
        }
    }
    let common_paths = [
        r"C:\Program Files\Java",
        r"C:\Program Files\Eclipse Adoptium",
        r"C:\Program Files\Microsoft\jdk-17",
        r"C:\Program Files\BellSoft",
        r"C:\Program Files\Zulu",
    ];
    for base in &common_paths {
        if let Ok(entries) = std::fs::read_dir(base) {
            for entry in entries.flatten() {
                let java_bin = entry.path().join("bin").join("java.exe");
                if java_bin.exists() {
                    candidates.push(java_bin.to_string_lossy().to_string());
                }
            }
        }
    }

    for cand in &candidates {
        match java_major_version(cand) {
            Some(v) if java_version_supported(v) => {
                log::info!("Using Java {} at: {}", v, cand);
                return Some(cand.clone());
            }
            Some(v) => {
                // 跳过但继续找：机器上可能同时装了 26(Path)和 21(Program Files)
                log::warn!("Skipping Java {} at {} (Spring Boot 3.3.7 supports 17-21)", v, cand);
            }
            None => {
                log::warn!("Cannot determine Java version at: {}", cand);
            }
        }
    }
    None
}

fn wait_for_ready(port: u16, timeout_secs: u64) -> bool {
    let start = Instant::now();
    let timeout = Duration::from_secs(timeout_secs);
    loop {
        if let Ok(mut stream) = TcpStream::connect(format!("127.0.0.1:{}", port)) {
            use std::io::{Read, Write};
            stream.set_read_timeout(Some(Duration::from_secs(2))).ok();
            stream.set_write_timeout(Some(Duration::from_secs(2))).ok();
            let req = format!("GET /health HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n");
            if stream.write_all(req.as_bytes()).is_ok() {
                let mut buf = [0u8; 64];
                if stream.read(&mut buf).is_ok() {
                    let resp = String::from_utf8_lossy(&buf);
                    if resp.contains("200") {
                        return true;
                    }
                }
            }
        }
        if start.elapsed() > timeout {
            return false;
        }
        std::thread::sleep(Duration::from_millis(500));
    }
}

fn kill_java_tree(child: &mut Child) {
    let pid = child.id();
    #[cfg(target_os = "windows")]
    {
        let _ = Command::new("taskkill")
            .args(["/F", "/T", "/PID", &pid.to_string()])
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .status();
    }
    #[cfg(not(target_os = "windows"))]
    {
        let _ = child.kill();
    }
    log::info!("Java process tree killed (PID: {})", pid);
}

fn kill_by_pid_marker(data_dir: &PathBuf) {
    let marker = data_dir.join("data").join(".java_pid");
    if let Ok(content) = std::fs::read_to_string(&marker) {
        if let Ok(pid) = content.trim().parse::<u32>() {
            log::info!("Killing Java PID {} via marker", pid);
            #[cfg(target_os = "windows")]
            {
                let _ = Command::new("taskkill")
                    .args(["/F", "/T", "/PID", &pid.to_string()])
                    .stdout(Stdio::null())
                    .stderr(Stdio::null())
                    .status();
            }
        }
    }
    let _ = std::fs::remove_file(&marker);
}

// ── PID marker (port-conflict detection) ─────────────────────────

fn write_pid_marker(data_dir: &PathBuf, java_pid: u32) {
    let marker = data_dir.join("data").join(".java_pid");
    if let Err(e) = std::fs::write(&marker, java_pid.to_string()) {
        log::warn!("Failed to write PID marker {:?}: {}", marker, e);
    }
}

fn is_our_java_running(data_dir: &PathBuf) -> bool {
    let marker = data_dir.join("data").join(".java_pid");
    if let Ok(content) = std::fs::read_to_string(&marker) {
        if let Ok(pid) = content.trim().parse::<u32>() {
            // Check if the process is alive (Windows: use tasklist)
            #[cfg(target_os = "windows")]
            {
                let output = Command::new("tasklist")
                    .args(["/FI", &format!("PID eq {}", pid), "/NH"])
                    .stdout(Stdio::piped())
                    .stderr(Stdio::null())
                    .output();
                if let Ok(out) = output {
                    let text = String::from_utf8_lossy(&out.stdout);
                    return text.contains("java.exe");
                }
            }
            #[cfg(not(target_os = "windows"))]
            {
                let output = Command::new("kill")
                    .args(["-0", &pid.to_string()])
                    .stdout(Stdio::null())
                    .stderr(Stdio::null())
                    .status();
                return output.map(|s| s.success()).unwrap_or(false);
            }
        }
    }
    false
}

fn remove_pid_marker(data_dir: &PathBuf) {
    let marker = data_dir.join("data").join(".java_pid");
    let _ = std::fs::remove_file(&marker);
}

// ── single-instance lock ──────────────────────────────────────────

/// Check if a process with the given PID is alive and is our app.exe.
/// 只认 app.exe：避免旧锁文件里的 PID 被无关进程（msiexec 等）复用，
/// 导致误判"已在运行中"而拒绝启动。
fn is_process_running(pid: u32) -> bool {
    #[cfg(target_os = "windows")]
    {
        let output = Command::new("tasklist")
            .args(["/FI", &format!("PID eq {}", pid), "/FO", "CSV", "/NH"])
            .stdout(Stdio::piped())
            .stderr(Stdio::null())
            .output();
        if let Ok(out) = output {
            let text = String::from_utf8_lossy(&out.stdout);
            return text.to_lowercase().contains("app.exe");
        }
    }
    #[cfg(not(target_os = "windows"))]
    {
        let output = Command::new("kill")
            .args(["-0", &pid.to_string()])
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .status();
        return output.map(|s| s.success()).unwrap_or(false);
    }
    false
}

/// Find the PID listening on the given port (Windows netstat).
#[cfg(target_os = "windows")]
fn find_pid_on_port(port: u16) -> Option<u32> {
    let output = Command::new("netstat")
        .args(["-ano"])
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .output()
        .ok()?;
    let text = String::from_utf8_lossy(&output.stdout);
    for line in text.lines() {
        let line = line.trim();
        if line.to_uppercase().contains("LISTENING") && line.contains(&format!(":{} ", port)) {
            if let Some(pid_str) = line.split_whitespace().next_back() {
                if let Ok(pid) = pid_str.parse::<u32>() {
                    if pid != 0 {
                        return Some(pid);
                    }
                }
            }
        }
    }
    None
}

/// Check whether the given PID is a java.exe process (Windows tasklist).
#[cfg(target_os = "windows")]
fn process_is_java(pid: u32) -> bool {
    let output = Command::new("tasklist")
        .args(["/FI", &format!("PID eq {}", pid), "/FO", "CSV", "/NH"])
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .output();
    if let Ok(out) = output {
        let text = String::from_utf8_lossy(&out.stdout);
        return text.to_lowercase().contains("java.exe");
    }
    false
}

/// Try to acquire the single-instance lock.
/// Returns true if this instance successfully acquired the lock.
fn try_acquire_lock(lock_path: &std::path::Path) -> bool {
    let my_pid = std::process::id();
    if lock_path.exists() {
        if let Ok(content) = fs::read_to_string(lock_path) {
            if let Ok(pid) = content.trim().parse::<u32>() {
                // 锁文件里的 PID 就是当前进程自己 → 上次实例崩溃/被强杀留下的残留，
                // 直接清除（Windows 会复用 PID，否则会误判"已在运行"拒绝启动）
                if pid == my_pid {
                    log::warn!("Lock file contains our own PID {} (stale from crash/kill), removing", pid);
                    let _ = fs::remove_file(lock_path);
                } else if is_process_running(pid) {
                    log::warn!("Another instance is already running (PID: {})", pid);
                    return false;
                } else {
                    log::info!("Stale lock file found (PID {} is dead), removing", pid);
                    let _ = fs::remove_file(lock_path);
                }
            }
        }
    }
    // Write our PID
    if let Err(e) = fs::write(lock_path, my_pid.to_string()) {
        log::warn!("Failed to write lock file {:?}: {}", lock_path, e);
        return false;
    }
    log::info!("Single-instance lock acquired (PID: {})", my_pid);
    true
}

fn release_lock(lock_path: &std::path::Path) {
    let _ = fs::remove_file(lock_path);
    log::info!("Single-instance lock released");
}

// ── crash log: 上次会话异常退出（卡死强制重启/崩溃）→ 下次启动自动存日志到桌面 ──
//
// 整机卡死时应用代码无法执行，所以用"会话标记"检测：
//   1. 每次启动写 data/.app_running
//   2. 正常退出（托盘退出 / 退出码 0）删除标记
//   3. 下次启动发现标记还在 → 上次没走正常退出 → 把 startup.log + java.log
//      合并成一个 `YYYYMMDD_HHMMSS_崩溃日志.log` 存到桌面，方便非技术用户直接发送。
const CRASH_MARKER: &str = ".app_running";

/// 本地时间戳 YYYYMMDD_HHMMSS_SSS（含毫秒，避免同一秒多次崩溃文件名互相覆盖）
fn now_stamp() -> String {
    #[cfg(target_os = "windows")]
    unsafe {
        let mut st: SYSTEMTIME = std::mem::zeroed();
        GetLocalTime(&mut st);
        return format!("{:04}{:02}{:02}_{:02}{:02}{:02}_{:03}",
            st.w_year, st.w_month, st.w_day, st.w_hour, st.w_minute, st.w_second, st.w_milliseconds);
    }
    #[cfg(not(target_os = "windows"))]
    "00000000_000000_000".to_string()
}

/// 解析用户桌面目录（优先 SHGetKnownFolderPath 处理 OneDrive 重定向，失败退回 USERPROFILE\Desktop）
fn desktop_dir() -> Option<PathBuf> {
    #[cfg(target_os = "windows")]
    {
        // FOLDERID_Desktop = {B4BFCC3A-DB2C-424C-B029-7FE99A87C641}
        let desktop_fid = GUID {
            data1: 0xB4BFCC3A, data2: 0xDB2C, data3: 0x424C,
            data4: [0xB0, 0x29, 0x7F, 0xE9, 0x9A, 0x87, 0xC6, 0x41],
        };
        let mut path: *mut u16 = std::ptr::null_mut();
        let hr = unsafe { SHGetKnownFolderPath(&desktop_fid, 0, 0, &mut path) };
        if hr == 0 && !path.is_null() {
            use std::os::windows::ffi::OsStringExt;
            let len = unsafe { (0..).take_while(|&i| *path.offset(i) != 0).count() };
            let s = std::ffi::OsString::from_wide(unsafe { std::slice::from_raw_parts(path, len) });
            unsafe { CoTaskMemFree(path as *mut core::ffi::c_void); }
            let p = PathBuf::from(s);
            if p.exists() {
                return Some(p);
            }
        }
    }
    std::env::var("USERPROFILE").ok().map(|up| PathBuf::from(up).join("Desktop"))
}

/// 读取文件尾部最多 max_lines 行（崩溃日志只关心最近状态）
fn read_file_tail(path: &std::path::Path, max_lines: usize) -> String {
    match std::fs::read_to_string(path) {
        Ok(content) => {
            let lines: Vec<&str> = content.lines().collect();
            if lines.len() > max_lines {
                let mut s = format!("... (已截断前 {} 行，共 {} 行) ...\n", lines.len() - max_lines, lines.len());
                s.push_str(&lines[lines.len() - max_lines..].join("\n"));
                s
            } else {
                content
            }
        }
        Err(e) => format!("(无法读取: {})", e),
    }
}

/// 把 startup.log + java.log 合并成一个崩溃日志存到桌面。返回生成的文件路径。
fn save_crash_logs(data_dir: &PathBuf, app_version: &str) -> Option<PathBuf> {
    let desktop = desktop_dir()?;
    let stamp = now_stamp();
    let out_path = desktop.join(format!("{}_崩溃日志.log", stamp));

    let mut content = String::new();
    content.push_str("================ 顺居门业管理系统 崩溃日志 ================\n");
    content.push_str(&format!("应用版本: {}\n", app_version));
    content.push_str(&format!("日志生成时间: {}\n", stamp));
    content.push_str(&format!("数据目录: {}\n", data_dir.display()));
    content.push_str("说明: 上一次软件启动异常退出（可能是电脑卡死被强制重启），\n");
    content.push_str("      此文件用于技术人员排查问题，请原样发送，无需打开查看。\n");
    content.push_str("\n");

    content.push_str("================ startup.log（应用壳诊断） ================\n");
    content.push_str(&read_file_tail(&data_dir.join("startup.log"), 500));
    content.push_str("\n\n");

    content.push_str("================ java.log（后端日志，最近部分） ================\n");
    content.push_str(&read_file_tail(&data_dir.join("java.log"), 2000));
    content.push_str("\n");

    match std::fs::write(&out_path, content) {
        Ok(_) => Some(out_path),
        Err(e) => {
            log::warn!("Failed to write crash log to desktop: {}", e);
            diag_log(data_dir, &format!("crash log write failed: {}", e));
            None
        }
    }
}

fn write_crash_marker(data_dir: &PathBuf) {
    let _ = std::fs::write(data_dir.join(CRASH_MARKER), now_stamp());
}

fn clear_crash_marker(data_dir: &PathBuf) {
    let _ = std::fs::remove_file(data_dir.join(CRASH_MARKER));
}

fn previous_session_crashed(data_dir: &PathBuf) -> bool {
    data_dir.join(CRASH_MARKER).exists()
}

// ── Win32 tray icon (pure FFI — Shell_NotifyIcon + CreatePopupMenu;
//    no comctl32 dependency, works even on stripped/Ghost Windows) ──

const WM_TRAYICON: UINT = WM_USER + 1;
static TRAY_APP: std::sync::OnceLock<tauri::AppHandle> = std::sync::OnceLock::new();

/// Encode a Rust &str to a null‑terminated wide string.
fn wide_null(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

/// Convert RGBA bytes to HICON (adapted from winit/tray-icon).
unsafe fn rgba_to_hicon(rgba: &[u8], width: u32, height: u32) -> HICON {
    let count = (width * height) as usize;
    let mut bgra = vec![0u8; count * 4];
    for i in 0..count {
        let o = i * 4;
        bgra[o] = rgba[o + 2];    // B ← R
        bgra[o + 1] = rgba[o + 1]; // G
        bgra[o + 2] = rgba[o];    // R ← B
        bgra[o + 3] = rgba[o + 3]; // A
    }
    let and_mask: Vec<u8> = (0..count).map(|i| bgra[i * 4 + 3].wrapping_sub(255)).collect();
    CreateIcon(0, width as i32, height as i32, 1, 32, and_mask.as_ptr(), bgra.as_ptr())
}

unsafe extern "system" fn tray_wndproc(hwnd: HWND, msg: UINT, wparam: WPARAM, lparam: LPARAM) -> LRESULT {
    match msg {
        WM_TRAYICON => match lparam as u32 {
            WM_RBUTTONUP => {
                log::info!("[tray] right-click received");
                let menu = CreatePopupMenu();
                let show_wide = wide_null("显示主窗口");
                let quit_wide = wide_null("退出");
                InsertMenuW(menu, 0, MF_STRING, 1001, show_wide.as_ptr());
                InsertMenuW(menu, 1, MF_STRING, 1002, quit_wide.as_ptr());
                SetForegroundWindow(hwnd);
                let mut pt = POINT { x: 0, y: 0 };
                GetCursorPos(&mut pt);
                let cmd = TrackPopupMenu(menu, TPM_RIGHTBUTTON | TPM_BOTTOMALIGN | TPM_RETURNCMD, pt.x, pt.y, 0, hwnd, std::ptr::null());
                DestroyMenu(menu);
                log::info!("[tray] menu returned cmd={}", cmd);
                match cmd as u32 {
                    1001 => { if let Some(a) = TRAY_APP.get() { if let Some(w) = a.get_webview_window("main") { let _ = w.show(); let _ = w.set_focus(); } } }
                    1002 => { log::info!("[tray] quit requested"); if let Some(a) = TRAY_APP.get() { shutdown_app(a); } }
                    _ => {}
                }
            }
            WM_LBUTTONUP => {
                if let Some(app) = TRAY_APP.get() {
                    if let Some(w) = app.get_webview_window("main") { let _ = w.show(); let _ = w.set_focus(); }
                }
            }
            _ => {}
        },
        WM_COMMAND => match wparam as u32 {
            1001 => { if let Some(a) = TRAY_APP.get() { if let Some(w) = a.get_webview_window("main") { let _ = w.show(); let _ = w.set_focus(); } } }
            1002 => { log::info!("[tray] quit via WM_COMMAND"); if let Some(a) = TRAY_APP.get() { shutdown_app(a); } }
            _ => {}
        },
        // 正常关机时 Windows 会发 WM_QUERYENDSESSION → WM_ENDSESSION。
        // 此时清掉崩溃标记，避免下次开机误报崩溃日志；真正卡死强制重启
        // 收不到这两个消息 → 标记残留 → 下次启动自动存日志。这样区分"正常关机"和"死机强启"。
        WM_QUERYENDSESSION => { return 1; } // 返回 TRUE 允许系统关机/重启，避免阻塞关机
        WM_ENDSESSION => {
            if wparam != 0 {
                log::info!("Windows shutting down (WM_ENDSESSION), clearing crash marker");
                if let Some(a) = TRAY_APP.get() {
                    if let Some(data_dir) = a.try_state::<DataDir>() {
                        clear_crash_marker(&data_dir.0);
                    }
                }
            }
        }
        WM_DESTROY => { PostQuitMessage(0); }
        _ => return DefWindowProcW(hwnd, msg, wparam, lparam),
    }
    0
}

fn init_win32_tray(app_handle: tauri::AppHandle, icon_rgba: &[u8], w: u32, h: u32) {
    TRAY_APP.set(app_handle.clone()).ok();
    let rgba_owned = icon_rgba.to_vec();

    std::thread::spawn(move || unsafe {
        let hicon = rgba_to_hicon(&rgba_owned, w, h);
        let hi = GetModuleHandleW(std::ptr::null());
        let class_wide = wide_null("ShunjuTrayClass");
        let wc = WNDCLASSW {
            style: CS_HREDRAW | CS_VREDRAW,
            lpfn_wndproc: Some(tray_wndproc as _),
            cb_cls_extra: 0, cb_wnd_extra: 0,
            h_instance: hi,
            h_icon: 0, h_cursor: LoadCursorW(0, IDC_ARROW),
            hbr_background: 0,
            menu_name: std::ptr::null(),
            class_name: class_wide.as_ptr(),
        };
        RegisterClassW(&wc);

        // 父窗口用 NULL（隐藏的顶级窗口）而不是 HWND_MESSAGE：
        // message-only 窗口收不到 WM_QUERYENDSESSION/WM_ENDSESSION 等系统广播，
        // 导致正常关机时崩溃标记不清理、下次开机误报崩溃日志。
        // 顶级隐藏窗口同样能挂托盘图标（Shell_NotifyIcon），且能收到关机广播。
        let hwnd = CreateWindowExW(
            0, class_wide.as_ptr(), std::ptr::null(),
            WS_OVERLAPPEDWINDOW,
            CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
            0, 0, hi, std::ptr::null_mut(),
        );

        let mut nid: NOTIFYICONDATAW = std::mem::zeroed();
        nid.cb_size = std::mem::size_of::<NOTIFYICONDATAW>() as u32;
        nid.hwnd = hwnd;
        nid.u_flags = NIF_MESSAGE | NIF_TIP | NIF_ICON;
        nid.u_callback_message = WM_TRAYICON;
        nid.h_icon = hicon;
        let tip = wide_null("顺居门业管理系统");
        let tlen = std::cmp::min(tip.len(), 128);
        nid.sz_tip[..tlen].copy_from_slice(&tip[..tlen]);

        let add_ok = Shell_NotifyIconW(NIM_ADD, &mut nid);
        log::info!("[tray] tray window hwnd={}, Shell_NotifyIcon add={}", hwnd, add_ok);

        let mut msg: MSG = std::mem::zeroed();
        while GetMessageW(&mut msg, 0, 0, 0) != 0 {
            TranslateMessage(&msg);
            DispatchMessageW(&msg);
        }

        Shell_NotifyIconW(NIM_DELETE, &mut nid);
        DestroyIcon(hicon);
        DestroyWindow(hwnd);
    });
}

// ── Java spawn / monitor ─────────────────────────────────────────

/// Open java.log, rotating it when it grows beyond 2MB.
/// Prevents unbounded log accumulation across runs and upgrades.
fn open_java_log(log_path: &PathBuf) -> std::fs::File {
    if let Ok(meta) = std::fs::metadata(log_path) {
        if meta.len() > 2 * 1024 * 1024 {
            let old = log_path.with_extension("log.old");
            let _ = std::fs::remove_file(&old);
            let _ = std::fs::rename(log_path, &old);
        }
    }
    std::fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(log_path)
        .unwrap_or_else(|e| {
            log::warn!("Cannot open java.log: {}, using null", e);
            std::fs::File::create("NUL").unwrap()
        })
}

/// Detect total physically installed RAM in KB (Windows). Returns None on failure.
fn system_ram_kb() -> Option<u64> {
    #[cfg(target_os = "windows")]
    {
        let mut kb: u64 = 0;
        let ok = unsafe { GetPhysicallyInstalledSystemMemory(&mut kb) };
        if ok != 0 && kb > 0 {
            return Some(kb);
        }
    }
    None
}

/// Pick JVM memory args that scale with the machine's physical RAM.
/// The previous hard-coded `-Xmx512m` could, together with WebView2 + the shell,
/// exhaust a small/stripped machine's RAM at startup -> whole-desktop freeze.
/// Capping the heap (a hard ceiling) makes it physically impossible for the app
/// to eat more than the machine can afford. Returns (profile label, args).
fn compute_java_mem_args() -> (String, Vec<String>) {
    let ram_gb = system_ram_kb().map(|kb| kb as f64 / 1024.0 / 1024.0);
    match ram_gb {
        Some(gb) if gb < 3.0 => (
            format!("low-ram({:.0}GB)", gb),
            vec![
                "-Xmx256m".to_string(),
                "-Xms64m".to_string(),
                "-XX:MaxMetaspaceSize=160m".to_string(),
                "-XX:+UseSerialGC".to_string(),
                "-XX:TieredStopAtLevel=1".to_string(),
            ],
        ),
        Some(gb) if gb < 6.0 => (
            format!("mid-ram({:.0}GB)", gb),
            vec![
                "-Xmx384m".to_string(),
                "-Xms64m".to_string(),
                "-XX:MaxMetaspaceSize=192m".to_string(),
                "-XX:+UseSerialGC".to_string(),
                "-XX:TieredStopAtLevel=1".to_string(),
            ],
        ),
        _ => (
            format!("ample-ram({:?}GB)", ram_gb),
            vec![
                "-Xmx512m".to_string(),
                "-Xms128m".to_string(),
                "-XX:MaxMetaspaceSize=256m".to_string(),
            ],
        ),
    }
}

fn spawn_java(config: &JavaSpawnConfig) -> Option<Child> {
    let java_log_file = open_java_log(&config.java_log);
    let java_log_file_err = java_log_file.try_clone().unwrap_or_else(|_| {
        std::fs::File::create("NUL").unwrap()
    });

    let install_dir_prop = format!("-Dapp.install-dir={}", config.install_dir);
    // 堆内存按机器实际内存自适应（见 compute_java_mem_args），避免低配机启动即卡死；
    // 保留 ExitOnOutOfMemoryError：真 OOM 时直接退出以便监控线程重启，而不是挂死
    let mut args = config.mem_args.clone();
    args.push("-XX:+ExitOnOutOfMemoryError".to_string());
    args.push("-Dfile.encoding=UTF-8".to_string());
    args.push("-Dsun.jnu.encoding=UTF-8".to_string());
    args.push(install_dir_prop);
    args.push("-jar".to_string());
    args.push(config.jar_str.clone());
    args.push("--spring.profiles.active=local".to_string());
    let mut cmd = Command::new(&config.java_cmd);
    cmd.args(&args)
        .current_dir(&config.data_dir)
        .stdout(java_log_file)
        .stderr(java_log_file_err);

    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        cmd.creation_flags(0x08000000); // CREATE_NO_WINDOW
    }

    match cmd.spawn() {
        Ok(mut child) => {
            log::info!("Java spawned (PID: {:?}), waiting for port 8080...", child.id());
            if wait_for_ready(8080, 60) {
                log::info!("Java backend ready on port 8080");
                Some(child)
            } else {
                log::error!("Java startup timeout");
                kill_java_tree(&mut child);
                None
            }
        }
        Err(e) => {
            log::error!("Failed to spawn Java: {}", e);
            None
        }
    }
}

fn start_java_monitor(
    app_handle: tauri::AppHandle,
    java_process: JavaProcess,
    config: Arc<JavaSpawnConfig>,
    shutdown_flag: Arc<AtomicBool>,
) {
    std::thread::spawn(move || {
        let mut restart_count: u32 = 0;
        // 待重启标志：Java 退出 或 上次 spawn 失败 时为 true。
        // 修复"spawn 失败后 guard 置 None → 下一轮永远不再重启 → 后端静默死亡"的 bug。
        let mut restart_pending = false;

        loop {
            std::thread::sleep(Duration::from_secs(MONITOR_INTERVAL_SECS));

            if shutdown_flag.load(Ordering::SeqCst) {
                log::info!("Shutdown flag set, monitor exiting");
                return;
            }

            // Check if the process is still alive
            let child_exited = {
                let mut guard = java_process.lock().unwrap();
                match *guard {
                    Some(ref mut child) => {
                        match child.try_wait() {
                            Ok(Some(status)) => {
                                log::warn!("Java process exited with status: {}", status);
                                let _ = child.wait();
                                *guard = None;
                                remove_pid_marker(&config.data_dir);
                                true
                            }
                            Ok(None) => false,
                            Err(e) => {
                                log::error!("Error checking Java process: {}", e);
                                false
                            }
                        }
                    }
                    None => false,
                }
            };
            if child_exited {
                restart_pending = true;
            }
            if !restart_pending {
                continue;
            }

            // 需要重启（Java 退出 或 上次 spawn 失败）：计数并尝试，直到成功或超限
            restart_count += 1;
            if restart_count > MAX_RESTART_ATTEMPTS {
                log::error!("Java crashed {} times, giving up", MAX_RESTART_ATTEMPTS);
                diag_log(&config.data_dir, "java crashed repeatedly, giving up");
                show_error_dialog(&format!(
                    "Java 后端反复崩溃（已重试{}次），请检查 java.log 日志后重启程序。",
                    MAX_RESTART_ATTEMPTS
                ));
                // Kill Java process tree and clean up before exit
                {
                    let mut guard = java_process.lock().unwrap();
                    if let Some(ref mut child) = *guard {
                        kill_java_tree(child);
                    }
                    *guard = None;
                }
                kill_by_pid_marker(&config.data_dir);
                release_lock(&config.data_dir.join("app.lock"));
                app_handle.exit(1);
                return;
            }

            log::warn!("Attempting Java restart ({}/{})...", restart_count, MAX_RESTART_ATTEMPTS);

            // Re-check shutdown flag before the long spawn_java call
            if shutdown_flag.load(Ordering::SeqCst) {
                log::info!("Shutdown requested during restart, aborting");
                return;
            }

            if TcpStream::connect("127.0.0.1:8080").is_ok() {
                log::warn!("Port 8080 still in use after crash, waiting 10s for release...");
                std::thread::sleep(Duration::from_secs(10));
                if shutdown_flag.load(Ordering::SeqCst) {
                    return;
                }
            }

            // 防重启风暴：JVM 崩溃后固定等 5 秒再拉起，避免连崩连拉时启动高峰叠加拖垮弱机
            std::thread::sleep(Duration::from_secs(5));

            match spawn_java(&config) {
                Some(mut new_child) => {
                    // Final check before storing — if app is exiting, kill the new child
                    if shutdown_flag.load(Ordering::SeqCst) {
                        log::info!("Shutdown during spawn, killing newly spawned Java");
                        kill_java_tree(&mut new_child);
                        return;
                    }
                    let pid = new_child.id();
                    let mut guard = java_process.lock().unwrap();
                    *guard = Some(new_child);
                    write_pid_marker(&config.data_dir, pid);
                    log::info!("Java restarted successfully (attempt {})", restart_count);
                    restart_count = 0;
                    restart_pending = false;
                }
                None => {
                    // spawn 失败：restart_pending 保持 true，下一轮继续重试，直到成功或超限
                    log::error!("Java restart failed (attempt {}), will retry", restart_count);
                }
            }
        }
    });
}

// ── shutdown helper (shared by close handler and tray menu) ──────

fn shutdown_app(app: &tauri::AppHandle) {
    log::info!("=== Shutting down application ===");

    if let Some(flag) = app.try_state::<ShutdownFlag>() {
        flag.0.store(true, Ordering::SeqCst);
    }

    if let Some(jp) = app.try_state::<JavaProcess>() {
        if let Ok(mut guard) = jp.try_lock() {
            if let Some(ref mut child) = *guard {
                kill_java_tree(child);
            }
            *guard = None;
        }
    }

    if let Some(data_dir) = app.try_state::<DataDir>() {
        kill_by_pid_marker(&data_dir.0);
        release_lock(&data_dir.0.join("app.lock"));
        clear_crash_marker(&data_dir.0);
    }

    app.exit(0);
}

// ── entry point ──────────────────────────────────────────────────

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(
            tauri_plugin_log::Builder::default()
                .level(log::LevelFilter::Info)
                .build(),
        )
        .invoke_handler(tauri::generate_handler![save_file])
        .setup(|app| {
            // ── Windows Job Object: kill children when parent exits ──
            #[cfg(target_os = "windows")]
            setup_job_object();

            let exe_dir = std::env::current_exe()
                .ok()
                .and_then(|p| p.parent().map(|d| d.to_path_buf()))
                .unwrap_or_else(|| PathBuf::from("."));

            // ── resolve data directory ──
            let mut data_dir = if cfg!(debug_assertions) {
                PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("..")
            } else if let Ok(custom) = std::env::var("WINDOW_DATA_DIR") {
                PathBuf::from(custom)
            } else {
                exe_dir.clone()
            };

            // ── 关键：安装在 Program Files 时，数据目录必须稳定到 %APPDATA% ──
            // Program Files 是写保护目录。若数据放这里，会随"是否管理员启动"在不同位置跳：
            // 管理员跑→写进 Program Files；普通用户跑→写不进→退回 APPDATA，两个位置数据
            // 不一致，重启后可能打开空的数据库，看起来像"软件没了/没安装成功"（表哥实测
            // C 盘装有问题、D 盘装的没事）。判断用路径（含 "Program Files"）而非运行时
            // 权限，保证无论提权与否数据位置都确定。
            let exe_lower = exe_dir.to_string_lossy().to_lowercase();
            if exe_lower.contains("program files") {
                let app_data = std::env::var("APPDATA")
                    .map(PathBuf::from)
                    .unwrap_or_else(|_| exe_dir.clone());
                let forced = app_data.join("com.shunjumc.window-system");
                std::fs::create_dir_all(forced.join("data"))
                    .unwrap_or_else(|e| show_error_and_exit(&format!("无法创建数据目录: {}", e)));
                log::warn!("安装在 Program Files，数据固定存到: {:?}", forced);
                data_dir = forced;
            } else {
                data_dir = match std::fs::create_dir_all(data_dir.join("data")) {
                    Ok(_) => {
                        let test_file = data_dir.join("data").join(".write_test");
                        if std::fs::write(&test_file, "ok").is_ok() {
                            let _ = std::fs::remove_file(&test_file);
                            Ok(data_dir)
                        } else {
                            Err(std::io::Error::new(std::io::ErrorKind::PermissionDenied, "not writable"))
                        }
                    }
                    Err(e) => Err(e),
                }.unwrap_or_else(|_| {
                    let app_data = std::env::var("APPDATA")
                        .map(PathBuf::from)
                        .unwrap_or_else(|_| exe_dir.clone());
                    let fallback = app_data.join("com.shunjumc.window-system");
                    std::fs::create_dir_all(fallback.join("data"))
                        .unwrap_or_else(|e| show_error_and_exit(&format!("无法创建数据目录: {}", e)));
                    log::warn!("安装目录无写入权限，数据存储到: {:?}", fallback);
                    fallback
                });
            }

            // ── 通用数据迁移：新数据目录为空时，从旧的候选位置（安装目录 或 APPDATA）迁移数据 ──
            // 双向兜底：Program Files→APPDATA、以及旧 APPDATA 数据→新 D 盘安装，都覆盖，
            // 避免用户切换安装位置/升级后打开空库、"数据消失"。
            migrate_if_empty(&data_dir, &exe_dir);

            log::info!("数据目录: {:?}", data_dir);
            diag_log(&data_dir, &format!("data dir ok: {:?}", data_dir));

            // ── single-instance lock ──
            let lock_path = data_dir.join("app.lock");
            if !try_acquire_lock(&lock_path) {
                diag_log(&data_dir, "single-instance lock held by another instance");
                show_error_and_exit("顺居门业管理系统已在运行中。");
            }

            // ── 崩溃日志：上次会话异常退出（卡死强制重启/崩溃）→ 自动把日志存到桌面 ──
            // 必须放在这里（WebView2/Java 启动之前）：如果这台机器"每次打开都卡死"，
            // 日志也要在卡死发生前先存好；否则卡死循环下日志永远存不下来。
            if previous_session_crashed(&data_dir) {
                log::info!("Previous session did not shut down cleanly -> saving crash logs to Desktop");
                let version = app.package_info().version.to_string();
                match save_crash_logs(&data_dir, &version) {
                    Some(p) => {
                        log::info!("Crash log saved to Desktop: {:?}", p);
                        diag_log(&data_dir, &format!("crash log saved to Desktop: {:?}", p));
                    }
                    None => diag_log(&data_dir, "crash log save FAILED (Desktop unavailable?)"),
                }
            }
            write_crash_marker(&data_dir);

            // ── port-conflict detection & stale-Java cleanup ──
            // 覆盖安装/异常退出后，可能残留旧的 Java 占着 8080 和数据库锁。
            // 尽量识别并杀掉它，避免新实例启动失败或两套进程互相争抢。
            let already_running = TcpStream::connect("127.0.0.1:8080").is_ok();

            if already_running {
                let stale_ours = is_our_java_running(&data_dir);
                let stale_java_pid = find_pid_on_port(8080).filter(|pid| process_is_java(*pid));

                if stale_ours || stale_java_pid.is_some() {
                    log::info!("Stale Java on port 8080 (ours={}, pid={:?}); cleaning up", stale_ours, stale_java_pid);
                    if stale_ours {
                        kill_by_pid_marker(&data_dir);
                    } else if let Some(pid) = stale_java_pid {
                        let _ = Command::new("taskkill")
                            .args(["/F", "/T", "/PID", &pid.to_string()])
                            .stdout(Stdio::null())
                            .stderr(Stdio::null())
                            .status();
                    }
                    // 清理残留的 H2 数据库锁文件，避免 "database may be already in use"
                    let h2_lock = data_dir.join("data").join("window_db.lock.db");
                    if h2_lock.exists() {
                        log::info!("Removing stale H2 lock file: {:?}", h2_lock);
                        let _ = std::fs::remove_file(&h2_lock);
                    }
                    // Wait for port release
                    for _ in 0..20 {
                        if TcpStream::connect("127.0.0.1:8080").is_err() { break; }
                        std::thread::sleep(Duration::from_millis(500));
                    }
                    if TcpStream::connect("127.0.0.1:8080").is_ok() {
                        diag_log(&data_dir, "port 8080 stuck after stale-java cleanup");
                        show_error_and_exit("端口 8080 被占用且无法释放，请手动关闭 Java 进程后重试。");
                    }
                } else {
                    log::warn!("Port 8080 is occupied by another application");
                    diag_log(&data_dir, "port 8080 occupied by another application");
                    show_error_and_exit("端口 8080 已被其他程序占用，请关闭后重试。");
                }
            }

            // 走到这里时 8080 空闲。清理上次强杀 Java 后可能残留的 H2 锁文件，
            // 避免新 Java 启动报 "database may be already in use"
            let h2_lock = data_dir.join("data").join("window_db.lock.db");
            if h2_lock.exists() {
                log::info!("Removing stale H2 lock file: {:?}", h2_lock);
                let _ = std::fs::remove_file(&h2_lock);
            }

            // ── find Java & JAR ──
            let jar_path = find_jar_path(app);
            log::info!("JAR path: {:?}", jar_path);
            if !jar_path.exists() {
                diag_log(&data_dir, &format!("jar not found: {:?}", jar_path));
                show_error_and_exit(&format!("找不到 JAR 文件: {:?}", jar_path));
            }

            let java_cmd = find_java().unwrap_or_else(|| {
                diag_log(&data_dir, "java not found (PATH/JAVA_HOME/common paths)");
                show_error_and_exit("未找到可用的 Java 运行环境。\n\n请安装 Java 17 或 21（JDK 26 等过新版本与系统不兼容，已自动跳过）。");
            });
            log::info!("Using Java: {}", java_cmd);

            // ── 按机器实际内存选 JVM 堆参数（低配机防卡死）──
            let (mem_profile, mem_args) = compute_java_mem_args();
            log::info!("JVM memory profile [{}]: {:?}", mem_profile, mem_args);
            diag_log(&data_dir, &format!("JVM memory profile [{}]: {}", mem_profile, mem_args.join(" ")));

            // 安装目录 = JAR 所在目录的上一级（JAR 在 resources/ 子目录）
            let install_dir = jar_path.parent()
                .and_then(|p| p.parent())
                .map(|p| p.to_string_lossy().to_string())
                .unwrap_or_default();

            let spawn_config = Arc::new(JavaSpawnConfig {
                java_cmd,
                jar_str: jar_path.to_string_lossy().to_string(),
                install_dir,
                data_dir: data_dir.clone(),
                java_log: data_dir.join("java.log"),
                mem_args,
            });

            // ── 先创建主窗口（加载页），后端就绪后导航到真实地址 ──
            // 避免后端未就绪时黑屏等待（最长 60 秒）被误认为"卡死"
            let real_url = tauri::Url::parse("http://localhost:8080/#/admin").unwrap();
            let loading_url = build_loading_url();
            let window = match tauri::WebviewWindowBuilder::new(
                app,
                "main",
                tauri::WebviewUrl::External(tauri::Url::parse(&loading_url).unwrap()),
            )
            .title("顺居门业管理系统")
            .inner_size(1280.0, 800.0)
            .min_inner_size(1024.0, 680.0)
            .resizable(true)
            .fullscreen(false)
            .center()
            // 精简系统/老显卡上 WebView2 硬件加速合成会整窗黑屏卡死 → 强制软件渲染。
            // 这是 Tauri/WebView2 黑屏问题最常用的修复。
            // 另给渲染进程 V8 堆设硬上限，防止前端页面在低配机上吃掉过多内存（V8 默认上限
            // 随物理内存走，能到 2GB+，低配机上会叠加 JVM 一起压爆整机）。
            .additional_browser_args("--disable-gpu --disable-gpu-compositing --js-flags=--max-old-space-size=384")
            // Let the webview receive native HTML5 drag-and-drop (used by the
            // import dialog). Tauri's default handler would swallow dropped files.
            .disable_drag_drop_handler()
            .build()
            {
                Ok(w) => w,
                Err(e) => {
                    diag_log(&data_dir, &format!("window creation failed: {}", e));
                    show_error_and_exit(&format!(
                        "无法创建主窗口（WebView2 初始化失败）。\n\n\
                         精简系统可能缺少 WebView2 Runtime 或显卡驱动异常，导致窗口黑屏。\n\
                         请安装/更新 WebView2 Runtime 后重试：\n\
                         https://developer.microsoft.com/microsoft-edge/webview2/\n\n\
                         错误详情: {}\n\n\
                         诊断日志: {}\\startup.log",
                        e,
                        data_dir.display()
                    ))
                }
            };
            diag_log(&data_dir, "window created (loading page)");
            let window_for_java = window.clone();

            // ── Java 进程状态（后台线程就绪后填充）──
            let java_process: JavaProcess = Arc::new(Mutex::new(None));
            app.manage(java_process.clone());

            // ── start crash monitor ──
            let shutdown_flag = Arc::new(AtomicBool::new(false));
            app.manage(ShutdownFlag(shutdown_flag.clone()));
            start_java_monitor(
                app.handle().clone(),
                java_process.clone(),
                spawn_config.clone(),
                shutdown_flag.clone(),
            );

            // ── 后台启动 Java，就绪后导航窗口到管理后台 ──
            let data_dir_for_java = data_dir.clone();
            std::thread::spawn(move || {
                diag_log(&data_dir_for_java, "spawning java backend...");
                match spawn_java(&spawn_config) {
                    Some(child) => {
                        let pid = child.id();
                        write_pid_marker(&data_dir_for_java, pid);
                        {
                            let mut guard = java_process.lock().unwrap();
                            *guard = Some(child);
                        }
                        log::info!("Java ready, navigating window to admin");
                        diag_log(&data_dir_for_java, "java ready, navigating to admin");
                        let _ = window_for_java.navigate(real_url);
                    }
                    None => {
                        diag_log(&data_dir_for_java, "java startup FAILED (60s timeout), see java.log");
                        let log_content = std::fs::read_to_string(&spawn_config.java_log)
                            .unwrap_or_else(|_| "无法读取日志".to_string());
                        let last_lines: String = log_content.lines().rev().take(10).collect::<Vec<_>>().join("\n");
                        show_error_and_exit(&format!("Java 后端启动失败（60秒超时）。\n\n最近日志:\n{}", last_lines));
                    }
                }
            });

            // ── minimize to tray on window close instead of exiting ──
            let window_for_event = window.clone();
            window.on_window_event(move |event| {
                if let tauri::WindowEvent::CloseRequested { api, .. } = event {
                    log::info!("=== CloseRequested, minimizing to tray ===");
                    api.prevent_close();
                    if let Err(e) = window_for_event.hide() {
                        log::error!("Failed to hide window: {}", e);
                    }
                }
            });

            // ── system tray (Win32 Shell_NotifyIcon, no comctl32 dep) ──
            let tray_image = match app.default_window_icon() {
                Some(icon) => icon.clone(),
                None => tauri::image::Image::new_owned(vec![0u8; 32 * 32 * 4], 32, 32),
            };
            init_win32_tray(
                app.handle().clone(),
                tray_image.rgba(),
                tray_image.width(),
                tray_image.height(),
            );

            app.manage(DataDir(data_dir));

            Ok(())
        })
        .build(tauri::generate_context!())
        .expect("error while building tauri application")
        .run(|app_handle, event| {
            if let tauri::RunEvent::ExitRequested { code, .. } = event {
                let exit_code = code.unwrap_or(0);
                log::info!("=== ExitRequested (fallback, code={}) ===", exit_code);
                if let Some(flag) = app_handle.try_state::<ShutdownFlag>() {
                    flag.0.store(true, Ordering::SeqCst);
                }
                // Kill Java process tree before hard exit
                if let Some(jp) = app_handle.try_state::<JavaProcess>() {
                    if let Ok(mut guard) = jp.try_lock() {
                        if let Some(ref mut child) = *guard {
                            kill_java_tree(child);
                        }
                        *guard = None;
                    }
                }
                // Clean up PID marker and lock file. 正常退出（code 0）清掉崩溃标记；
                // 异常退出（非 0）保留标记，让下次启动把日志存到桌面。
                if let Some(data_dir) = app_handle.try_state::<DataDir>() {
                    kill_by_pid_marker(&data_dir.0);
                    release_lock(&data_dir.0.join("app.lock"));
                    if exit_code == 0 {
                        clear_crash_marker(&data_dir.0);
                    }
                }
                std::process::exit(exit_code);
            }
        });
}
