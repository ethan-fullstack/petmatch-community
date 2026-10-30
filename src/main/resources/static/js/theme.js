(() => {
    const storageKey = "petmatch-theme";
    const root = document.documentElement;
    const systemPreference = window.matchMedia("(prefers-color-scheme: light)");

    function savedPreference() {
        try {
            const value = window.localStorage.getItem(storageKey);
            return value === "light" || value === "dark" ? value : null;
        } catch (error) {
            return null;
        }
    }

    function selectedTheme() {
        return savedPreference() ?? (systemPreference.matches ? "light" : "dark");
    }

    function setTheme(theme) {
        root.dataset.theme = theme;
        root.style.colorScheme = theme;

        const toggle = document.getElementById("theme-toggle");
        if (toggle) {
            const target = theme === "light" ? "oscuro" : "claro";
            toggle.textContent = theme === "light" ? "☾ Modo oscuro" : "☀ Modo claro";
            toggle.setAttribute("aria-label", "Cambiar a modo " + target);
            toggle.setAttribute("title", "Cambiar a modo " + target);
            toggle.setAttribute("aria-pressed", String(theme === "light"));
        }
    }

    function saveTheme(theme) {
        try {
            window.localStorage.setItem(storageKey, theme);
        } catch (error) {
            // The current selection remains available even if storage is blocked.
        }
        setTheme(theme);
    }

    function createToggle() {
        const toggle = document.createElement("button");
        toggle.id = "theme-toggle";
        toggle.className = "pm-theme-toggle";
        toggle.type = "button";
        toggle.addEventListener("click", () => {
            saveTheme(root.dataset.theme === "light" ? "dark" : "light");
        });
        document.body.appendChild(toggle);
        setTheme(root.dataset.theme || selectedTheme());
    }

    setTheme(selectedTheme());
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", createToggle, { once: true });
    } else {
        createToggle();
    }

    if (typeof systemPreference.addEventListener === "function") {
        systemPreference.addEventListener("change", () => {
            if (!savedPreference()) setTheme(selectedTheme());
        });
    }
})();
