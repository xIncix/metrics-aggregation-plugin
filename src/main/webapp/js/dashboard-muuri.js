(function () {
    document.addEventListener('DOMContentLoaded', function () {
        let editMode = false;

        const grid = new Muuri('.dashboard-grid', {
            dragEnabled: true,

            dragStartPredicate: function (item, event) {
                if (!editMode) return false;
                return !event.target.closest('.delete-widget-btn');
            },

            layout: {
                fillGaps: true,
                rounding: true
            }
        });

        const configureBtn = document.getElementById('configure-dashboard-btn');
        const saveBtn = document.getElementById('save-dashboard-btn');

        if (!configureBtn || !saveBtn) {
            console.error('Dashboard buttons not found');
            return;
        }

        configureBtn.addEventListener('click', function () {
            editMode = true;
            document.body.classList.add('dashboard-edit-mode');

            configureBtn.style.display = 'none';
            saveBtn.style.display = 'inline-flex';

            grid.refreshItems().layout();
        });

        saveBtn.addEventListener('click', function () {
            editMode = false;
            document.body.classList.remove('dashboard-edit-mode');

            saveBtn.style.display = 'none';
            configureBtn.style.display = 'inline-flex';

            grid.refreshItems().layout();
        });

        const gridElement = document.querySelector('.dashboard-grid');

        gridElement.addEventListener('click', function (e) {
            const deleteBtn = e.target.closest('.delete-widget-btn');
            if (!deleteBtn) return;

            e.preventDefault();
            e.stopPropagation();

            const itemElem = deleteBtn.closest('.item');
            if (!itemElem) return;

            const item = grid.getItem(itemElem);
            if (!item) return;

            grid.remove([item], { removeElements: true });
        });

        setTimeout(function () {
            grid.refreshItems().layout();
        }, 300);
    });
})();
