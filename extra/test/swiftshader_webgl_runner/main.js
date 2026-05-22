const { app, BrowserWindow, ipcMain } = require("electron");
const fs = require("fs");
const path = require("path");

app.commandLine.appendSwitch("use-gl", "angle");
app.commandLine.appendSwitch("use-angle", "swiftshader");
app.commandLine.appendSwitch("enable-unsafe-swiftshader");
app.commandLine.appendSwitch("disable-gpu-sandbox");

const jobPath = process.argv[process.argv.length - 1];

ipcMain.once("swiftshader-result", (_event, result) => {
  console.log(JSON.stringify(result));
  app.quit();
});

ipcMain.handle("swiftshader-job", () => {
  const job = JSON.parse(fs.readFileSync(jobPath, "utf8"));
  job.runnerDir = __dirname;
  return job;
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
