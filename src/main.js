import { UIManager } from './ui/UIManager.js';
import { ProjectManager } from './core/ProjectManager.js';

class LuminaApp {
    constructor() {
        this.projectManager = new ProjectManager();
        this.ui = new UIManager(this);
    }

    async init() {
        console.log("Lumina Lucky Imager initializing...");
        this.ui.init();
        this.ui.setStatus("Application Ready");
    }
}

const app = new LuminaApp();
window.addEventListener('DOMContentLoaded', () => {
    app.init();
});

export { app };
