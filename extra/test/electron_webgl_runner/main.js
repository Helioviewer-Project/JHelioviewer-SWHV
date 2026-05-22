const { app, BrowserWindow, ipcMain } = require("electron");
const fs = require("fs");
const path = require("path");

const backend = process.env.JHV_ELECTRON_GL_BACKEND || "swiftshader";
if (backend === "swiftshader") {
  app.commandLine.appendSwitch("use-gl", "angle");
  app.commandLine.appendSwitch("use-angle", "swiftshader");
  app.commandLine.appendSwitch("enable-unsafe-swiftshader");
  app.commandLine.appendSwitch("disable-gpu-sandbox");
}

const jobPath = process.argv[process.argv.length - 1];

ipcMain.once("electron-result", (_event, result) => {
  console.log(JSON.stringify(result));
  app.quit();
});

ipcMain.handle("electron-job", () => {
  const payload = JSON.parse(fs.readFileSync(jobPath, "utf8"));
  payload.runnerDir = __dirname;
  return payload;
});

app.whenReady().then(async () => {
  const win = new BrowserWindow({
    show: false,
    webPreferences: {
      offscreen: true,
      contextIsolation: false,
      nodeIntegration: true,
    },
  });

  await win.loadFile(path.join(__dirname, "runner.html"));
});
