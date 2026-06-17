document.addEventListener("DOMContentLoaded", function () {

    /* =========================
       Sidebar
    ========================= */

    const sidebar = document.getElementById('sidebar');
    const sidebarToggle = document.getElementById('sidebarToggle');
    const desktopSidebarToggle = document.getElementById('desktopSidebarToggle');
    const sidebarClose = document.getElementById('sidebarClose');
    const sidebarOverlay = document.getElementById('sidebarOverlay');
    
    

    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', () => {
            sidebar.classList.remove('-translate-x-full');
            sidebarOverlay.classList.remove('hidden');
        });
    }

    const closeSidebar = () => {
        sidebar.classList.add('-translate-x-full');
        sidebarOverlay.classList.add('hidden');
    };

    if (sidebarClose) {
        sidebarClose.addEventListener('click', closeSidebar);
    }

    if (sidebarOverlay) {
        sidebarOverlay.addEventListener('click', closeSidebar);
    }

    if (desktopSidebarToggle) {
        desktopSidebarToggle.addEventListener('click', () => {
            sidebar.classList.toggle('md:translate-x-0');
            sidebar.classList.toggle('md:-translate-x-full');

            if (sidebar.classList.contains('md:-translate-x-full')) {
                sidebar.classList.add('md:hidden');
            } else {
                setTimeout(() => {
                    sidebar.classList.remove('md:hidden');
                }, 10);
            }
        });
    }

    /* =========================
       Sidebar Menu Toggle
    ========================= */

    const menuToggles = document.querySelectorAll('.menu-toggle');

    // 1. 초기 로드 시 localStorage에서 열려있던 메뉴 복원
    menuToggles.forEach((toggle, index) => {
        const submenu = toggle.nextElementSibling;
        const arrowIcon = toggle.querySelector('.arrow-icon') || toggle.querySelector('svg:last-child');
        
        const isOpen = localStorage.getItem('sidebar_menu_' + index);
        if (isOpen === 'true' && submenu) {
            submenu.classList.remove('hidden');
            if (arrowIcon) arrowIcon.classList.add('rotate-180');
        }

        toggle.addEventListener('click', function () {
            if (submenu) {
                submenu.classList.toggle('hidden');
                const isHidden = submenu.classList.contains('hidden');
                localStorage.setItem('sidebar_menu_' + index, !isHidden);
            }

            if (arrowIcon) {
                arrowIcon.classList.toggle('rotate-180');
            }
        });
    });

    /* =========================
       Theme Toggle
    ========================= */

    const themeToggle = document.getElementById('themeToggle');
    const themeIcon = document.getElementById('themeIcon');
    const body = document.body;

    if (themeToggle) {
        themeToggle.addEventListener('click', () => {

            // 라이트 모드 토글
            body.classList.toggle('light-mode');

            // 아이콘 변경
            if (body.classList.contains('light-mode')) {
                themeIcon.textContent = '🌙';
            } else {
                themeIcon.textContent = '☀️';
            }

        });
    }

});