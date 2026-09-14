const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('ghost', {
  setInteractive: (value) => ipcRenderer.send('overlay:set-interactive', Boolean(value)),
  quit: () => ipcRenderer.send('app:quit'),
  onActiveWindowChanged: (callback) => {
    const listener = (_event, info) => callback(info);
    ipcRenderer.on('active-window:changed', listener);
    return () => ipcRenderer.removeListener('active-window:changed', listener);
  },
});
