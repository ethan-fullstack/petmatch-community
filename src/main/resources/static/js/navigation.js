(() => {
    function highlightCurrentPage() {
        const currentPath = window.location.pathname.replace(/\/+$/, "") || "/";
        const links = Array.from(document.querySelectorAll(".pm-nav__link"));
        const matching = links.filter((link) => {
            const target = new URL(link.href).pathname.replace(/\/+$/, "") || "/";
            return currentPath === target || (target !== "/" && currentPath.startsWith(target + "/"));
        });
        const selected = matching.sort((first, second) => second.pathname.length - first.pathname.length)[0];
        if (selected) {
            selected.setAttribute("aria-current", "page");
            selected.classList.add("pm-nav__link--active");
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", highlightCurrentPage, { once: true });
    } else {
        highlightCurrentPage();
    }
})();
