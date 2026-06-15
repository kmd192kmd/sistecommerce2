document.addEventListener("DOMContentLoaded", function () {

    const sidebar = document.getElementById("sidebar");
    const sidebarToggle = document.getElementById("sidebarToggle");
    const desktopSidebarToggle = document.getElementById("desktopSidebarToggle");
    const sidebarClose = document.getElementById("sidebarClose");
    const sidebarOverlay = document.getElementById("sidebarOverlay");

    sidebarToggle?.addEventListener("click", () => {
        sidebar.classList.remove("-translate-x-full");
        sidebarOverlay.classList.remove("hidden");
    });

    const closeSidebar = () => {
        sidebar.classList.add("-translate-x-full");
        sidebarOverlay.classList.add("hidden");
    };

    sidebarClose?.addEventListener("click", closeSidebar);
    sidebarOverlay?.addEventListener("click", closeSidebar);

    desktopSidebarToggle?.addEventListener("click", () => {

        if (sidebar.classList.contains("hidden")) {
            sidebar.classList.remove("hidden");
        } else {
            sidebar.classList.add("hidden");
        }

    });

    const menuToggles = document.querySelectorAll(".menu-toggle");

    menuToggles.forEach(toggle => {
        toggle.addEventListener("click", function () {

            const submenu = this.nextElementSibling;
            const arrowIcon = this.querySelector(".arrow-icon");

            submenu?.classList.toggle("hidden");
            arrowIcon?.classList.toggle("rotate-180");

        });
    });

    const themeToggle = document.getElementById("themeToggle");
    const themeIcon = document.getElementById("themeIcon");

    if (localStorage.getItem("theme") === "light") {
        document.body.classList.add("light-mode");

        if (themeIcon) {
            themeIcon.textContent = "🌙";
        }
    }

    themeToggle?.addEventListener("click", () => {

        document.body.classList.toggle("light-mode");

        if (document.body.classList.contains("light-mode")) {

            localStorage.setItem("theme", "light");

            if (themeIcon) {
                themeIcon.textContent = "🌙";
            }

        } else {

            localStorage.setItem("theme", "dark");

            if (themeIcon) {
                themeIcon.textContent = "☀️";
            }
        }
    });

});