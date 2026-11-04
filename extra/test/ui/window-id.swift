import AppKit
import CoreGraphics
import Foundation

let pid = Int(CommandLine.arguments[1])!
let title = CommandLine.arguments[2]
if let application = NSRunningApplication(processIdentifier: pid_t(pid)) {
    _ = application.activate(options: [])
    Thread.sleep(forTimeInterval: 0.2)
}
let windows = CGWindowListCopyWindowInfo(.optionAll, kCGNullWindowID) as! [[String: Any]]
for window in windows {
    if window[kCGWindowOwnerPID as String] as? Int == pid,
       window[kCGWindowName as String] as? String == title,
       window[kCGWindowLayer as String] as? Int == 0,
       let id = window[kCGWindowNumber as String] as? Int {
        print(id)
        exit(0)
    }
}
fputs("Native window not found for capture: \(title)\n", stderr)
exit(1)
